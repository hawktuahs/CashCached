package com.bt.accounts.service;

import com.bt.accounts.dto.*;
import com.bt.accounts.entity.AccountTransaction;
import com.bt.accounts.entity.CashCachedLedgerEntry;
import com.bt.accounts.entity.CashCachedWallet;
import com.bt.accounts.entity.FdAccount;
import com.bt.accounts.event.AccountRedemptionEvent;
import com.bt.accounts.exception.AccountNotFoundException;
import com.bt.accounts.exception.InvalidAccountDataException;
import com.bt.accounts.exception.ServiceIntegrationException;
import com.bt.accounts.repository.AccountTransactionRepository;
import com.bt.accounts.repository.CashCachedWalletRepository;
import com.bt.accounts.repository.FdAccountRepository;
import com.bt.accounts.time.TimeProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedemptionService {

    private final FdAccountRepository accountRepository;
    private final AccountTransactionRepository transactionRepository;
    private final CashCachedWalletRepository walletRepository;
    private final CashCachedService cashCachedService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final TimeProvider timeProvider;

    private static final String REDEMPTION_TYPE_MATURITY = "MATURITY";
    private static final String REDEMPTION_TYPE_PREMATURE = "PREMATURE";
    private static final BigDecimal PREMATURE_PENALTY_RATE = new BigDecimal("0.02");
    private static final int PREMATURE_PENALTY_DAYS = 30;

    @Cacheable(value = "redemptionEnquiry", key = "#accountNo")
    public RedemptionEnquiryResponse getRedemptionEnquiry(String accountNo) {
        FdAccount account = accountRepository.findByAccountNo(accountNo)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNo));

        if (account.getStatus() != FdAccount.AccountStatus.ACTIVE) {
            throw new InvalidAccountDataException("Account is not active: " + accountNo);
        }

        LocalDateTime now = getCurrentTime();
        LocalDateTime maturityDate = account.getMaturityDate();

        boolean isMatured = now.isAfter(maturityDate) || now.isEqual(maturityDate);
        long daysUntilMaturity = isMatured ? 0 : ChronoUnit.DAYS.between(now, maturityDate);
        long daysOverdue = isMatured ? ChronoUnit.DAYS.between(maturityDate, now) : 0;

        BigDecimal accruedInterest = calculateAccruedInterest(account, now);
        BigDecimal penaltyAmount = BigDecimal.ZERO;
        String penaltyReason = null;
        String redemptionEligibility = "ELIGIBLE";
        List<String> warnings = new ArrayList<>();

        if (!isMatured) {
            penaltyAmount = calculatePrematurePenalty(account, daysUntilMaturity);
            penaltyReason = "Premature redemption penalty (" + daysUntilMaturity + " days before maturity)";
            warnings.add("Account has not reached maturity date");
            warnings.add("Penalty of " + penaltyAmount + " tokens will be deducted");
            redemptionEligibility = "ELIGIBLE_WITH_PENALTY";
        }

        BigDecimal netPayableAmount = account.getPrincipalAmount()
                .add(accruedInterest)
                .subtract(penaltyAmount);

        CashCachedWallet wallet = walletRepository.findByCustomerId(account.getCustomerId())
                .orElse(null);
        BigDecimal currentWalletBalance = wallet != null ? wallet.getBalance() : BigDecimal.ZERO;
        boolean hasSufficientBalance = true;

        if (daysOverdue > 30) {
            warnings.add("Account is overdue by " + daysOverdue + " days");
        }

        return RedemptionEnquiryResponse.builder()
                .accountNo(accountNo)
                .customerId(account.getCustomerId())
                .productCode(account.getProductCode())
                .principalAmount(account.getPrincipalAmount())
                .interestRate(account.getInterestRate())
                .tenureMonths(account.getTenureMonths())
                .maturityDate(maturityDate)
                .currentDate(now)
                .isMatured(isMatured)
                .daysUntilMaturity(isMatured ? null : (int) daysUntilMaturity)
                .daysOverdue(isMatured ? (int) daysOverdue : null)
                .accruedInterest(accruedInterest)
                .maturityAmount(account.getMaturityAmount())
                .penaltyAmount(penaltyAmount)
                .netPayableAmount(netPayableAmount)
                .penaltyReason(penaltyReason)
                .currentWalletBalance(currentWalletBalance)
                .hasSufficientBalance(hasSufficientBalance)
                .redemptionEligibility(redemptionEligibility)
                .warnings(warnings.toArray(new String[0]))
                .build();
    }

    @Transactional
    @CacheEvict(value = { "accounts", "customerAccounts", "redemptionEnquiry" }, allEntries = true)
    public RedemptionResponse processRedemption(String accountNo, RedemptionRequest request) {
        FdAccount account = accountRepository.findByAccountNo(accountNo)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNo));

        if (account.getStatus() != FdAccount.AccountStatus.ACTIVE) {
            throw new InvalidAccountDataException("Account is not active: " + accountNo);
        }

        LocalDateTime now = getCurrentTime();
        LocalDateTime maturityDate = account.getMaturityDate();
        boolean isMatured = now.isAfter(maturityDate) || now.isEqual(maturityDate);
        long daysUntilMaturity = isMatured ? 0 : ChronoUnit.DAYS.between(now, maturityDate);

        String redemptionType = isMatured ? REDEMPTION_TYPE_MATURITY : REDEMPTION_TYPE_PREMATURE;

        BigDecimal accruedInterest = calculateAccruedInterest(account, now);
        BigDecimal penaltyAmount = isMatured ? BigDecimal.ZERO : calculatePrematurePenalty(account, daysUntilMaturity);
        BigDecimal netPayoutAmount = account.getPrincipalAmount()
                .add(accruedInterest)
                .subtract(penaltyAmount);

        String transactionId = UUID.randomUUID().toString();
        String beneficiaryCustomerId = account.getCustomerId();

        String blockchainTxHash;
        String walletAddress;
        BigDecimal newWalletBalance;

        try {
            CashCachedRedeemRequest redeemRequest = new CashCachedRedeemRequest();
            redeemRequest.setCustomerId(beneficiaryCustomerId);
            redeemRequest.setAmount(netPayoutAmount);
            redeemRequest.setReference("FD Redemption - " + accountNo + " - " + redemptionType);

            CashCachedLedgerEntry ledgerEntry = cashCachedService.redeem(redeemRequest);
            blockchainTxHash = ledgerEntry.getTransactionHash();

            CashCachedWallet wallet = walletRepository.findByCustomerId(beneficiaryCustomerId)
                    .orElseThrow(() -> new ServiceIntegrationException(
                            "Wallet not found for customer: " + beneficiaryCustomerId));
            walletAddress = wallet.getCustomerId();
            newWalletBalance = wallet.getBalance();

            log.info("Blockchain redemption successful. TxHash: {}, Amount: {}", blockchainTxHash, netPayoutAmount);

        } catch (Exception e) {
            log.error("Blockchain redemption failed for account: {}", accountNo, e);
            throw new ServiceIntegrationException("Failed to process blockchain redemption: " + e.getMessage());
        }

        AccountTransaction.TransactionType transactionType = isMatured
                ? AccountTransaction.TransactionType.MATURITY_PAYOUT
                : AccountTransaction.TransactionType.PREMATURE_CLOSURE;

        recordRedemptionTransaction(
                account,
                transactionId,
                transactionType,
                account.getPrincipalAmount(),
                accruedInterest,
                penaltyAmount,
                netPayoutAmount,
                blockchainTxHash,
                null);

        account.setStatus(FdAccount.AccountStatus.CLOSED);
        account.setClosedAt(now);
        account.setClosedBy(getCurrentUsername());
        account.setClosureReason(redemptionType + " redemption");
        account.setUpdatedAt(now);
        accountRepository.save(account);

        publishRedemptionEvent(account, redemptionType, transactionId, blockchainTxHash,
                accruedInterest, penaltyAmount, netPayoutAmount, daysUntilMaturity, null);

        log.info("Account {} redeemed successfully. Type: {}, NetPayout: {}, TxHash: {}",
                accountNo, redemptionType, netPayoutAmount, blockchainTxHash);

        return RedemptionResponse.builder()
                .accountNo(accountNo)
                .customerId(account.getCustomerId())
                .redemptionType(redemptionType)
                .transactionId(transactionId)
                .redemptionDate(now)
                .principalAmount(account.getPrincipalAmount())
                .interestAmount(accruedInterest)
                .penaltyAmount(penaltyAmount)
                .netPayoutAmount(netPayoutAmount)
                .blockchainTransactionHash(blockchainTxHash)
                .walletAddress(walletAddress)
                .newWalletBalance(newWalletBalance)
                .status("SUCCESS")
                .message("Account redeemed successfully")
                .build();
    }

    private BigDecimal calculateAccruedInterest(FdAccount account, LocalDateTime currentDate) {
        LocalDateTime startDate = account.getCreatedAt();
        LocalDateTime maturityDate = account.getMaturityDate();

        long totalDays = ChronoUnit.DAYS.between(startDate, maturityDate);
        long elapsedDays = ChronoUnit.DAYS.between(startDate, currentDate);

        if (elapsedDays >= totalDays) {
            return account.getMaturityAmount().subtract(account.getPrincipalAmount());
        }

        BigDecimal totalInterest = account.getMaturityAmount().subtract(account.getPrincipalAmount());
        BigDecimal dailyInterest = totalInterest.divide(BigDecimal.valueOf(totalDays), 10, RoundingMode.HALF_UP);
        BigDecimal accruedInterest = dailyInterest.multiply(BigDecimal.valueOf(elapsedDays));

        return accruedInterest.setScale(0, RoundingMode.HALF_UP);
    }

    private BigDecimal calculatePrematurePenalty(FdAccount account, long daysUntilMaturity) {
        if (daysUntilMaturity <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal penaltyBase = account.getPrincipalAmount();

        if (daysUntilMaturity < PREMATURE_PENALTY_DAYS) {
            BigDecimal proportionalRate = PREMATURE_PENALTY_RATE
                    .multiply(BigDecimal.valueOf(daysUntilMaturity))
                    .divide(BigDecimal.valueOf(PREMATURE_PENALTY_DAYS), 10, RoundingMode.HALF_UP);
            return penaltyBase.multiply(proportionalRate).setScale(0, RoundingMode.HALF_UP);
        }

        return penaltyBase.multiply(PREMATURE_PENALTY_RATE).setScale(0, RoundingMode.HALF_UP);
    }

    private void recordRedemptionTransaction(
            FdAccount account,
            String transactionId,
            AccountTransaction.TransactionType transactionType,
            BigDecimal principalAmount,
            BigDecimal interestAmount,
            BigDecimal penaltyAmount,
            BigDecimal netPayoutAmount,
            String blockchainTxHash,
            String reason) {

        try {
            String description = transactionType == AccountTransaction.TransactionType.MATURITY_PAYOUT
                    ? "Maturity payout for FD account"
                    : "Premature closure payout for FD account";

            if (reason != null && !reason.trim().isEmpty()) {
                description += " - " + reason;
            }

            AccountTransaction transaction = AccountTransaction.builder()
                    .transactionId(transactionId)
                    .accountNo(account.getAccountNo())
                    .transactionType(transactionType)
                    .amount(netPayoutAmount)
                    .balanceAfter(BigDecimal.ZERO)
                    .description(description)
                    .referenceNo(blockchainTxHash)
                    .processedBy(getCurrentUsername())
                    .transactionDate(LocalDateTime.now())
                    .build();

            transactionRepository.save(transaction);

            log.info("Recorded redemption transaction: {} for account: {}, Type: {}, Amount: {}",
                    transactionId, account.getAccountNo(), transactionType, netPayoutAmount);

        } catch (Exception e) {
            log.error("Failed to record redemption transaction for account: {}", account.getAccountNo(), e);
        }
    }

    private void publishRedemptionEvent(
            FdAccount account,
            String redemptionType,
            String transactionId,
            String blockchainTxHash,
            BigDecimal interestAmount,
            BigDecimal penaltyAmount,
            BigDecimal netPayoutAmount,
            long daysUntilMaturity,
            String reason) {

        try {
            CashCachedWallet wallet = walletRepository.findByCustomerId(account.getCustomerId()).orElse(null);

            AccountRedemptionEvent event = AccountRedemptionEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType("ACCOUNT_REDEEMED")
                    .timestamp(LocalDateTime.now())
                    .accountNo(account.getAccountNo())
                    .customerId(account.getCustomerId())
                    .productCode(account.getProductCode())
                    .redemptionType(redemptionType)
                    .principalAmount(account.getPrincipalAmount())
                    .interestAmount(interestAmount)
                    .penaltyAmount(penaltyAmount)
                    .netPayoutAmount(netPayoutAmount)
                    .transactionId(transactionId)
                    .blockchainTransactionHash(blockchainTxHash)
                    .walletAddress(wallet != null ? wallet.getCustomerId() : null)
                    .maturityDate(account.getMaturityDate())
                    .daysBeforeMaturity(redemptionType.equals(REDEMPTION_TYPE_PREMATURE) ? (int) daysUntilMaturity : 0)
                    .reason(reason)
                    .processedBy(getCurrentUsername())
                    .build();

            kafkaTemplate.send("account-events", event.getEventId(), event);

            log.info("Published redemption event for account: {}, EventId: {}",
                    account.getAccountNo(), event.getEventId());

        } catch (Exception e) {
            log.error("Failed to publish redemption event for account: {}", account.getAccountNo(), e);
        }
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : "SYSTEM";
    }

    private LocalDateTime getCurrentTime() {
        return LocalDateTime.ofInstant(timeProvider.now(), java.time.ZoneId.systemDefault());
    }
}
