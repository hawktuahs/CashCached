package com.bt.accounts.service;

import com.bt.accounts.dto.*;
import com.bt.accounts.entity.AccountTransaction;
import com.bt.accounts.entity.CashCachedLedgerEntry;
import com.bt.accounts.entity.FdAccount;
import com.bt.accounts.event.AccountRedemptionEvent;
import com.bt.accounts.exception.AccountNotFoundException;
import com.bt.accounts.exception.InvalidAccountDataException;
import com.bt.accounts.exception.ServiceIntegrationException;
import com.bt.accounts.repository.AccountTransactionRepository;
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
    private final CashCachedService cashCachedService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final TimeProvider timeProvider;

    private static final String REDEMPTION_TYPE_MATURITY = "MATURITY";
    private static final String REDEMPTION_TYPE_PREMATURE = "PREMATURE";

    @Cacheable(value = "redemptionEnquiry", key = "#accountNo")
    public RedemptionEnquiryResponse getRedemptionEnquiry(String accountNo, String authToken) {
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

        BigDecimal ledgerBalance = resolveLedgerBalance(accountNo);
        BigDecimal principalOriginal = account.getPrincipalAmount() != null ? account.getPrincipalAmount()
                : BigDecimal.ZERO;
        BigDecimal accruedInterest = ledgerBalance.subtract(principalOriginal);
        if (accruedInterest.compareTo(BigDecimal.ZERO) < 0) {
            accruedInterest = BigDecimal.ZERO;
        }
        BigDecimal penaltyAmount = BigDecimal.ZERO;
        String penaltyReason = null;
        String redemptionEligibility = "ELIGIBLE";
        List<String> warnings = new ArrayList<>();

        if (!isMatured) {
            penaltyAmount = calculatePrematurePenalty(account, ledgerBalance, daysUntilMaturity);
            penaltyReason = "Premature redemption penalty (" + daysUntilMaturity + " days before maturity)";
            warnings.add("Account has not reached maturity date");
            warnings.add("Penalty of " + penaltyAmount + " tokens will be deducted");
            redemptionEligibility = "ELIGIBLE_WITH_PENALTY";
        }

        BigDecimal netPayableAmount = ledgerBalance.subtract(penaltyAmount);

        String bearerToken = (authToken != null && !authToken.isBlank()) ? authToken : null;
        BigDecimal currentWalletBalance = cashCachedService.balance(account.getCustomerId(), bearerToken).getBalance();
        boolean hasSufficientBalance = true;

        if (daysOverdue > 30) {
            warnings.add("Account is overdue by " + daysOverdue + " days");
        }

        return RedemptionEnquiryResponse.builder()
                .accountNo(accountNo)
                .customerId(account.getCustomerId())
                .productCode(account.getProductCode())
                .principalAmount(principalOriginal)
                .interestRate(account.getInterestRate())
                .tenureMonths(account.getTenureMonths())
                .maturityDate(maturityDate)
                .currentDate(now)
                .isMatured(isMatured)
                .daysUntilMaturity(isMatured ? null : (int) daysUntilMaturity)
                .daysOverdue(isMatured ? (int) daysOverdue : null)
                .accruedInterest(accruedInterest)
                .currentBalance(ledgerBalance)
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
    public RedemptionResponse processRedemption(String accountNo, RedemptionRequest request, String authToken) {
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

        BigDecimal ledgerBalance = resolveLedgerBalance(accountNo);
        BigDecimal principalOriginal = account.getPrincipalAmount() != null ? account.getPrincipalAmount()
                : BigDecimal.ZERO;
        BigDecimal accruedInterest = ledgerBalance.subtract(principalOriginal);
        if (accruedInterest.compareTo(BigDecimal.ZERO) < 0) {
            accruedInterest = BigDecimal.ZERO;
        }
        BigDecimal penaltyAmount = isMatured ? BigDecimal.ZERO : calculatePrematurePenalty(account, ledgerBalance, daysUntilMaturity);
        BigDecimal netPayoutAmount = ledgerBalance.subtract(penaltyAmount);

        String transactionId = UUID.randomUUID().toString();
        String beneficiaryCustomerId = account.getCustomerId();

        String blockchainTxHash;
        String walletAddress;
        BigDecimal newWalletBalance;

        try {
            CashCachedIssueRequest issueRequest = new CashCachedIssueRequest();
            issueRequest.setCustomerId(beneficiaryCustomerId);
            issueRequest.setAmount(netPayoutAmount);
            issueRequest.setReference("FD Redemption - " + accountNo + " - " + redemptionType);

            CashCachedLedgerEntry ledgerEntry = cashCachedService.issue(issueRequest);
            blockchainTxHash = ledgerEntry.getTransactionHash();
            walletAddress = issueRequest.getCustomerId();
            newWalletBalance = ledgerEntry.getBalanceAfter();

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
                principalOriginal,
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
        account.setPrincipalAmount(BigDecimal.ZERO);
        account.setMaturityAmount(BigDecimal.ZERO);
        account.setTotalInterestAccrued(BigDecimal.ZERO);
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
                .principalAmount(principalOriginal)
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

    private BigDecimal resolveLedgerBalance(String accountNo) {
        List<AccountTransaction> transactions = transactionRepository
                .findByAccountNoOrderByTransactionDateDesc(accountNo);
        if (transactions == null || transactions.isEmpty()) {
            FdAccount account = accountRepository.findByAccountNo(accountNo)
                    .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNo));
            BigDecimal principal = account.getPrincipalAmount();
            return principal != null ? principal : BigDecimal.ZERO;
        }
        return transactions.get(0).getBalanceAfter();
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

    private BigDecimal calculatePrematurePenalty(FdAccount account, BigDecimal balanceBase, long daysUntilMaturity) {
        if (daysUntilMaturity <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal penaltyBase = balanceBase != null ? balanceBase : BigDecimal.ZERO;
        BigDecimal penaltyRate = resolvePenaltyRate(account);
        int graceDays = resolvePenaltyGraceDays(account);

        if (log.isDebugEnabled()) {
            log.debug("Evaluating premature penalty for account {}: base={}, rate={}, graceDays={}, daysUntilMaturity={}",
                    account.getAccountNo(), penaltyBase, penaltyRate, graceDays, daysUntilMaturity);
        }

        if (penaltyRate.compareTo(BigDecimal.ZERO) <= 0) {
            log.debug("Penalty rate <= 0 for account {}, returning zero", account.getAccountNo());
            return BigDecimal.ZERO;
        }

        if (daysUntilMaturity <= graceDays) {
            log.debug("Account {} within grace period ({} days <= {}), returning zero",
                    account.getAccountNo(), daysUntilMaturity, graceDays);
            return BigDecimal.ZERO;
        }

        long chargeableDays = daysUntilMaturity - graceDays;
        BigDecimal fullWindow = BigDecimal.valueOf(Math.max(1, graceDays > 0 ? graceDays : 30));

        if (chargeableDays < fullWindow.longValue()) {
            BigDecimal proportionalRate = penaltyRate
                    .multiply(BigDecimal.valueOf(chargeableDays))
                    .divide(fullWindow, 10, RoundingMode.HALF_UP);
            BigDecimal penalty = penaltyBase.multiply(proportionalRate).setScale(0, RoundingMode.HALF_UP);
            log.debug("Account {} proportional penalty: rate={} => penalty={}", account.getAccountNo(), proportionalRate, penalty);
            return penalty;
        }

        BigDecimal penalty = penaltyBase.multiply(penaltyRate).setScale(0, RoundingMode.HALF_UP);
        log.debug("Account {} full penalty: base={} * rate={} = {}", account.getAccountNo(), penaltyBase, penaltyRate, penalty);
        return penalty;
    }

    private BigDecimal resolvePenaltyRate(FdAccount account) {
        BigDecimal configured = account.getPrematurePenaltyRate();
        return configured != null ? configured : BigDecimal.ZERO;
    }

    private int resolvePenaltyGraceDays(FdAccount account) {
        Integer configured = account.getPrematurePenaltyGraceDays();
        return configured != null ? configured : 0;
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
                    .walletAddress(account.getCustomerId())
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
