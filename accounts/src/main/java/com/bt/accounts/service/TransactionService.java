package com.bt.accounts.service;

import com.bt.accounts.dto.TransactionRequest;
import com.bt.accounts.dto.TransactionResponse;
import com.bt.accounts.dto.CashCachedIssueRequest;
import com.bt.accounts.entity.AccountTransaction;
import com.bt.accounts.entity.FdAccount;
import com.bt.accounts.exception.AccountNotFoundException;
import com.bt.accounts.exception.InvalidAccountDataException;
import com.bt.accounts.exception.ServiceIntegrationException;
import com.bt.accounts.repository.AccountTransactionRepository;
import com.bt.accounts.repository.FdAccountRepository;
import com.bt.accounts.time.TimeProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final AccountTransactionRepository transactionRepository;
    private final FdAccountRepository accountRepository;
    private final CashCachedService cashCachedService;
    private final PricingRuleEvaluator pricingRuleEvaluator;
    private final CacheManager cacheManager;
    @Value("${self.txn.relaxed:false}")
    private boolean selfTxnRelaxed;

    @Transactional
    public TransactionResponse recordTransaction(String accountNo, TransactionRequest request) {
        return recordTransaction(accountNo, request, null);
    }

    public TransactionResponse recordTransaction(String accountNo, TransactionRequest request,
            LocalDateTime occurredAt) {
        FdAccount account = accountRepository.findByAccountNo(accountNo)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNo));

        if (account.getStatus() == FdAccount.AccountStatus.CLOSED) {
            throw new InvalidAccountDataException("Cannot record transaction on closed account: " + accountNo);
        }

        AccountTransaction.TransactionType type = AccountTransaction.TransactionType
                .valueOf(request.getTransactionType());

        if ("FIXED_DEPOSIT".equalsIgnoreCase(account.getProductType())
                && (AccountTransaction.TransactionType.DEPOSIT == type
                        || AccountTransaction.TransactionType.WITHDRAWAL == type)) {
            throw new InvalidAccountDataException(
                    "CashCached deposits or withdrawals are disabled for fixed deposit accounts. Please use redemption.");
        }

        String transactionId = generateTransactionId(accountNo);
        BigDecimal amountTokens = requireTokenAmount(request.getAmount());
        BigDecimal currentBalance = calculateCurrentBalance(accountNo);
        PricingRuleEvaluator.EvaluationResult pricing = applyPricingRules(account, currentBalance, null);
        BigDecimal newBalance = calculateNewBalance(currentBalance, type, amountTokens);

        if (!("FIXED_DEPOSIT".equalsIgnoreCase(account.getProductType())
                && AccountTransaction.TransactionType.INTEREST_CREDIT == type)) {
            reconcileWalletForTransaction(account, type, amountTokens, request.getReferenceNo());
        }
        applyPenaltyIfNeeded(account, pricing.getPenalty(), request.getReferenceNo());

        AccountTransaction transaction = AccountTransaction.builder()
                .transactionId(transactionId)
                .accountNo(accountNo)
                .transactionType(type)
                .amount(amountTokens)
                .balanceAfter(newBalance)
                .description(request.getDescription())
                .referenceNo(request.getReferenceNo())
                .processedBy(getCurrentUsername())
                .remarks(request.getRemarks())
                .build();

        if (occurredAt != null) {
            transaction.setTransactionDate(occurredAt);
        }

        AccountTransaction savedTransaction = transactionRepository.save(transaction);
        log.info("Recorded transaction: {} for account: {}", transactionId, accountNo);

        TransactionResponse response = TransactionResponse.fromEntity(savedTransaction);
        evictCaches(account);
        return response;
    }

    @Transactional
    public TransactionResponse recordSelfTransaction(String accountNo, TransactionRequest request, String userIdHeader,
            String authHeader) {
        FdAccount account = accountRepository.findByAccountNo(accountNo)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNo));

        if (account.getStatus() == FdAccount.AccountStatus.CLOSED) {
            throw new InvalidAccountDataException("Cannot record transaction on closed account: " + accountNo);
        }

        if ("FIXED_DEPOSIT".equalsIgnoreCase(account.getProductType())) {
            throw new InvalidAccountDataException(
                    "CashCached deposits or withdrawals are disabled for fixed deposit accounts. Please use redemption.");
        }

        String subject = getCurrentUsername();
        String userId = (userIdHeader != null && !userIdHeader.isBlank()) ? userIdHeader : getCurrentUserIdClaim();
        String acctCustomer = account.getCustomerId() != null ? account.getCustomerId().trim() : null;
        String acctCreatedBy = account.getCreatedBy() != null ? account.getCreatedBy().trim() : null;
        boolean authorized = false;
        if (selfTxnRelaxed || Boolean.parseBoolean(System.getenv().getOrDefault("SELF_TXN_RELAXED", "false"))) {
            authorized = true;
        }
        if (subject != null && acctCustomer != null && subject.trim().equalsIgnoreCase(acctCustomer))
            authorized = true;
        if (!authorized && userId != null && acctCustomer != null && userId.trim().equalsIgnoreCase(acctCustomer))
            authorized = true;
        if (!authorized && subject != null && acctCreatedBy != null && subject.trim().equalsIgnoreCase(acctCreatedBy))
            authorized = true;
        if (!authorized && userId != null && acctCreatedBy != null && userId.trim().equalsIgnoreCase(acctCreatedBy))
            authorized = true;
        if (!authorized && authHeader != null && !authHeader.isBlank()) {
            try {
                log.debug("Customer profile validation skipped - using Kafka instead");
            } catch (Exception ignore) {
            }
        }
        log.debug("recordSelfTransaction auth subject={}, userId={}, accountCustomerId={}, createdBy={}", subject,
                userId, acctCustomer, acctCreatedBy);
        if (!authorized) {
            throw new InvalidAccountDataException("Unauthorized transaction for account: " + accountNo);
        }

        AccountTransaction.TransactionType type = AccountTransaction.TransactionType
                .valueOf(request.getTransactionType());
        if (!(AccountTransaction.TransactionType.DEPOSIT == type
                || AccountTransaction.TransactionType.WITHDRAWAL == type)) {
            throw new InvalidAccountDataException("Only DEPOSIT or WITHDRAWAL allowed for self transactions");
        }

        BigDecimal amountTokens = requireTokenAmount(request.getAmount());
        BigDecimal currentBalance = calculateCurrentBalance(accountNo);
        PricingRuleEvaluator.EvaluationResult pricing = applyPricingRules(account, currentBalance, authHeader);
        BigDecimal newBalance = calculateNewBalance(currentBalance, type, amountTokens);
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidAccountDataException("Insufficient balance for withdrawal");
        }

        if (authHeader != null && !authHeader.isBlank()) {
            try {
                log.debug("Product validation skipped - using Kafka instead");
            } catch (Exception ignored) {
            }
        }

        String transactionId = generateTransactionId(accountNo);

        AccountTransaction transaction = AccountTransaction.builder()
                .transactionId(transactionId)
                .accountNo(accountNo)
                .transactionType(type)
                .amount(amountTokens)
                .balanceAfter(newBalance)
                .description(request.getDescription())
                .referenceNo(request.getReferenceNo())
                .processedBy(subject)
                .remarks(request.getRemarks())
                .build();

        reconcileWalletForTransaction(account, type, amountTokens, request.getReferenceNo());

        AccountTransaction saved = transactionRepository.save(transaction);
        applyPenaltyIfNeeded(account, pricing.getPenalty(), request.getReferenceNo());
        evictCaches(account);
        return TransactionResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getAccountTransactions(String accountNo) {
        if (!accountRepository.existsByAccountNo(accountNo)) {
            throw new AccountNotFoundException("Account not found: " + accountNo);
        }

        List<AccountTransaction> transactions = transactionRepository
                .findByAccountNoOrderByTransactionDateDesc(accountNo);

        return transactions.stream()
                .map(TransactionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getAccountTransactionsByDateRange(
            String accountNo, LocalDateTime startDate, LocalDateTime endDate) {

        if (!accountRepository.existsByAccountNo(accountNo)) {
            throw new AccountNotFoundException("Account not found: " + accountNo);
        }

        List<AccountTransaction> transactions = transactionRepository
                .findByAccountNoAndDateRange(accountNo, startDate, endDate);

        return transactions.stream()
                .map(TransactionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    private BigDecimal calculateCurrentBalance(String accountNo) {
        List<AccountTransaction> transactions = transactionRepository
                .findByAccountNoOrderByTransactionDateDesc(accountNo);
        if (transactions.isEmpty()) {
            FdAccount account = accountRepository.findByAccountNo(accountNo).orElseThrow();
            BigDecimal pa = account.getPrincipalAmount();
            return pa != null ? pa : BigDecimal.ZERO;
        }
        return transactions.get(0).getBalanceAfter();
    }

    private BigDecimal calculateNewBalance(BigDecimal currentBalance, AccountTransaction.TransactionType type,
            BigDecimal amountTokens) {
        return switch (type) {
            case DEPOSIT, INTEREST_CREDIT -> currentBalance.add(amountTokens);
            case WITHDRAWAL, PENALTY_DEBIT, PREMATURE_CLOSURE, MATURITY_PAYOUT ->
                currentBalance.subtract(amountTokens);
            case REVERSAL -> currentBalance;
        };
    }

    private PricingRuleEvaluator.EvaluationResult applyPricingRules(FdAccount account, BigDecimal balance,
            String authToken) {
        PricingRuleEvaluator.EvaluationResult result;
        try {
            result = pricingRuleEvaluator.evaluate(account, balance, authToken);
        } catch (ServiceIntegrationException ex) {
            log.warn("Pricing evaluation failed for account {}: {}", account.getAccountNo(), ex.getMessage());
            return PricingRuleEvaluator.EvaluationResult.noRule(account.getBaseInterestRate());
        }

        BigDecimal appliedRate = result.getAppliedRate();
        boolean hasRule = result.getRule() != null;

        if (appliedRate != null
                && (account.getInterestRate() == null || account.getInterestRate().compareTo(appliedRate) != 0)) {
            account.setInterestRate(appliedRate);
        } else if (!hasRule && account.getBaseInterestRate() != null) {
            account.setInterestRate(account.getBaseInterestRate());
        }

        if (hasRule) {
            account.setActivePricingRuleId(result.getRule().getId());
            account.setActivePricingRuleName(result.getRule().getRuleName());
            account.setPricingRuleAppliedAt(TimeProvider.currentDateTime());
        } else {
            account.setActivePricingRuleId(null);
            account.setActivePricingRuleName(null);
            account.setPricingRuleAppliedAt(null);
        }

        return result;
    }

    private void applyPenaltyIfNeeded(FdAccount account, BigDecimal penalty, String reference) {
        if (penalty == null || penalty.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        String accountNo = account.getAccountNo();
        String txnId = generateTransactionId(accountNo);

        AccountTransaction penaltyTxn = AccountTransaction.builder()
                .transactionId(txnId)
                .accountNo(accountNo)
                .transactionType(AccountTransaction.TransactionType.PENALTY_DEBIT)
                .amount(penalty.negate())
                .balanceAfter(calculateNewBalance(calculateCurrentBalance(accountNo),
                        AccountTransaction.TransactionType.PENALTY_DEBIT, penalty))
                .description("Pricing rule penalty")
                .referenceNo(reference)
                .processedBy(getCurrentUsername())
                .remarks("Auto-applied pricing penalty")
                .build();
        penaltyTxn = transactionRepository.save(penaltyTxn);
        reconcileWalletForTransaction(account, AccountTransaction.TransactionType.PENALTY_DEBIT, penalty, reference);
        log.info("Applied penalty {} to account {} due to pricing rule", penalty, accountNo);
        evictCaches(account);
    }

    private String generateTransactionId(String accountNo) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String uuid = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return String.format("TXN-%s-%s-%s", accountNo, timestamp, uuid);
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : "system";
    }

    private BigDecimal requireTokenAmount(BigDecimal amount) {
        if (amount == null) {
            throw new InvalidAccountDataException("Amount is required");
        }
        try {
            BigDecimal normalized = amount.stripTrailingZeros();
            BigDecimal tokens = normalized.setScale(0, java.math.RoundingMode.UNNECESSARY);
            if (tokens.compareTo(BigDecimal.ONE) < 0) {
                throw new InvalidAccountDataException("Amount must be at least 1 CashCached token");
            }
            return tokens;
        } catch (ArithmeticException ex) {
            throw new InvalidAccountDataException("Amount must be a whole CashCached token");
        }
    }

    private void reconcileWalletForTransaction(FdAccount account, AccountTransaction.TransactionType type,
            BigDecimal amountTokens, String reference) {
        String customerId = account.getCustomerId();
        String txnReference = reference != null ? reference : account.getAccountNo();
        if (AccountTransaction.TransactionType.DEPOSIT == type) {
            cashCachedService.debitWallet(customerId, amountTokens, "Deposit to account " + txnReference);
        } else if (AccountTransaction.TransactionType.WITHDRAWAL == type
                || AccountTransaction.TransactionType.PREMATURE_CLOSURE == type
                || AccountTransaction.TransactionType.MATURITY_PAYOUT == type) {
            cashCachedService.creditWallet(customerId, amountTokens, "Withdrawal from account " + txnReference);
        }
    }

    private String getCurrentUserIdClaim() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null)
            return null;
        Object principal = authentication.getPrincipal();
        if (principal instanceof java.util.Map<?, ?> map) {
            Object id = map.get("id");
            if (id == null)
                id = map.get("userId");
            if (id == null)
                id = map.get("user_id");
            if (id == null)
                id = map.get("sub");
            if (id == null)
                id = map.get("preferred_username");
            if (id != null)
                return String.valueOf(id);
        }
        Object details = authentication.getDetails();
        if (details instanceof java.util.Map<?, ?> map) {
            Object id = map.get("id");
            if (id == null)
                id = map.get("userId");
            if (id == null)
                id = map.get("user_id");
            if (id == null)
                id = map.get("sub");
            if (id == null)
                id = map.get("preferred_username");
            if (id != null)
                return String.valueOf(id);
        }
        String name = authentication.getName();
        if (name != null && !name.isBlank())
            return name;
        return null;
    }

    private void evictCaches(FdAccount account) {
        if (cacheManager == null || account == null) {
            return;
        }
        Cache accountsCache = cacheManager.getCache("accounts");
        if (accountsCache != null) {
            accountsCache.evict(account.getAccountNo());
        }
        Cache customerAccountsCache = cacheManager.getCache("customerAccounts");
        if (customerAccountsCache != null) {
            customerAccountsCache.evict(account.getCustomerId());
        }
        Cache redemptionCache = cacheManager.getCache("redemptionEnquiry");
        if (redemptionCache != null) {
            redemptionCache.evict(account.getAccountNo());
        }
    }
}
