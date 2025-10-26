package com.bt.accounts.service;

import com.bt.accounts.dto.TransactionRequest;
import com.bt.accounts.dto.TransactionResponse;
import com.bt.accounts.entity.AccountTransaction;
import com.bt.accounts.entity.FdAccount;
import com.bt.accounts.exception.AccountNotFoundException;
import com.bt.accounts.exception.InvalidAccountDataException;
import com.bt.accounts.client.CustomerServiceClient;
import com.bt.accounts.client.ProductServiceClient;
import com.bt.accounts.client.ProductDto;
import com.bt.accounts.dto.ApiResponse;
import com.bt.accounts.repository.AccountTransactionRepository;
import com.bt.accounts.repository.FdAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final AccountTransactionRepository transactionRepository;
    private final FdAccountRepository accountRepository;
    private final CustomerServiceClient customerServiceClient;
    private final ProductServiceClient productServiceClient;
    @Value("${self.txn.relaxed:false}")
    private boolean selfTxnRelaxed;

    @Transactional
    public TransactionResponse recordTransaction(String accountNo, TransactionRequest request) {
        FdAccount account = accountRepository.findByAccountNo(accountNo)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNo));

        if (account.getStatus() == FdAccount.AccountStatus.CLOSED) {
            throw new InvalidAccountDataException("Cannot record transaction on closed account: " + accountNo);
        }

        String transactionId = generateTransactionId(accountNo);

        BigDecimal currentBalance = calculateCurrentBalance(accountNo);
        BigDecimal newBalance = calculateNewBalance(currentBalance, request);

        AccountTransaction transaction = AccountTransaction.builder()
                .transactionId(transactionId)
                .accountNo(accountNo)
                .transactionType(AccountTransaction.TransactionType.valueOf(request.getTransactionType()))
                .amount(request.getAmount())
                .balanceAfter(newBalance)
                .description(request.getDescription())
                .referenceNo(request.getReferenceNo())
                .processedBy(getCurrentUsername())
                .remarks(request.getRemarks())
                .build();

        AccountTransaction savedTransaction = transactionRepository.save(transaction);
        log.info("Recorded transaction: {} for account: {}", transactionId, accountNo);

        return TransactionResponse.fromEntity(savedTransaction);
    }

    @Transactional
    public TransactionResponse recordSelfTransaction(String accountNo, TransactionRequest request, String userIdHeader, String authHeader) {
        FdAccount account = accountRepository.findByAccountNo(accountNo)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNo));

        if (account.getStatus() == FdAccount.AccountStatus.CLOSED) {
            throw new InvalidAccountDataException("Cannot record transaction on closed account: " + accountNo);
        }

        String subject = getCurrentUsername();
        String userId = (userIdHeader != null && !userIdHeader.isBlank()) ? userIdHeader : getCurrentUserIdClaim();
        String acctCustomer = account.getCustomerId() != null ? account.getCustomerId().trim() : null;
        String acctCreatedBy = account.getCreatedBy() != null ? account.getCreatedBy().trim() : null;
        boolean authorized = false;
        if (selfTxnRelaxed || Boolean.parseBoolean(System.getenv().getOrDefault("SELF_TXN_RELAXED", "false"))) {
            authorized = true;
        }
        if (subject != null && acctCustomer != null && subject.trim().equalsIgnoreCase(acctCustomer)) authorized = true;
        if (!authorized && userId != null && acctCustomer != null && userId.trim().equalsIgnoreCase(acctCustomer)) authorized = true;
        if (!authorized && subject != null && acctCreatedBy != null && subject.trim().equalsIgnoreCase(acctCreatedBy)) authorized = true;
        if (!authorized && userId != null && acctCreatedBy != null && userId.trim().equalsIgnoreCase(acctCreatedBy)) authorized = true;
        if (!authorized && authHeader != null && !authHeader.isBlank()) {
            try {
                Map<String, Object> profile = customerServiceClient.getCurrentProfile(authHeader);
                if (profile != null) {
                    Object pid = profile.get("id");
                    if (pid != null && acctCustomer != null && String.valueOf(pid).trim().equalsIgnoreCase(acctCustomer)) {
                        authorized = true;
                    }
                    if (!authorized) {
                        Object uname = profile.get("username");
                        if (uname != null && subject != null && String.valueOf(uname).trim().equalsIgnoreCase(subject.trim())) {
                            authorized = true;
                        }
                    }
                }
            } catch (Exception ignore) {}
        }
        log.debug("recordSelfTransaction auth subject={}, userId={}, accountCustomerId={}, createdBy={}", subject, userId, acctCustomer, acctCreatedBy);
        if (!authorized) {
            throw new InvalidAccountDataException("Unauthorized transaction for account: " + accountNo);
        }

        String type = request.getTransactionType();
        if (!("DEPOSIT".equals(type) || "WITHDRAWAL".equals(type))) {
            throw new InvalidAccountDataException("Only DEPOSIT or WITHDRAWAL allowed for self transactions");
        }

        BigDecimal currentBalance = calculateCurrentBalance(accountNo);
        BigDecimal newBalance = calculateNewBalance(currentBalance, request);
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidAccountDataException("Insufficient balance for withdrawal");
        }

        if (authHeader != null && !authHeader.isBlank()) {
            try {
                ApiResponse<ProductDto> prodResp = productServiceClient.getProductByCode(account.getProductCode(), authHeader);
                ProductDto product = prodResp != null ? prodResp.getData() : null;
                if (product != null) {
                    if ("DEPOSIT".equals(type) && product.getMaxAmount() != null && newBalance.compareTo(product.getMaxAmount()) > 0) {
                        throw new InvalidAccountDataException("Exceeds product maximum amount");
                    }
                    if ("WITHDRAWAL".equals(type) && product.getMinAmount() != null && newBalance.compareTo(product.getMinAmount()) < 0) {
                        throw new InvalidAccountDataException("Below product minimum amount");
                    }
                }
            } catch (Exception ignored) {}
        }

        String transactionId = generateTransactionId(accountNo);

        AccountTransaction transaction = AccountTransaction.builder()
                .transactionId(transactionId)
                .accountNo(accountNo)
                .transactionType(AccountTransaction.TransactionType.valueOf(type))
                .amount(request.getAmount())
                .balanceAfter(newBalance)
                .description(request.getDescription())
                .referenceNo(request.getReferenceNo())
                .processedBy(subject)
                .remarks(request.getRemarks())
                .build();

        AccountTransaction saved = transactionRepository.save(transaction);
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
        List<AccountTransaction> transactions = transactionRepository.findByAccountNoOrderByTransactionDateDesc(accountNo);
        if (transactions.isEmpty()) {
            FdAccount account = accountRepository.findByAccountNo(accountNo).orElseThrow();
            BigDecimal pa = account.getPrincipalAmount();
            return pa != null ? pa : BigDecimal.ZERO;
        }
        return transactions.get(0).getBalanceAfter();
    }

    private BigDecimal calculateNewBalance(BigDecimal currentBalance, TransactionRequest request) {
        AccountTransaction.TransactionType type = AccountTransaction.TransactionType
                .valueOf(request.getTransactionType());

        return switch (type) {
            case DEPOSIT, INTEREST_CREDIT -> currentBalance.add(request.getAmount());
            case WITHDRAWAL, PENALTY_DEBIT, PREMATURE_CLOSURE, MATURITY_PAYOUT ->
                currentBalance.subtract(request.getAmount());
            case REVERSAL -> currentBalance;
        };
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

    private String getCurrentUserIdClaim() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) return null;
        Object principal = authentication.getPrincipal();
        if (principal instanceof java.util.Map<?, ?> map) {
            Object id = map.get("id");
            if (id == null) id = map.get("userId");
            if (id == null) id = map.get("user_id");
            if (id == null) id = map.get("sub");
            if (id == null) id = map.get("preferred_username");
            if (id != null) return String.valueOf(id);
        }
        Object details = authentication.getDetails();
        if (details instanceof java.util.Map<?, ?> map) {
            Object id = map.get("id");
            if (id == null) id = map.get("userId");
            if (id == null) id = map.get("user_id");
            if (id == null) id = map.get("sub");
            if (id == null) id = map.get("preferred_username");
            if (id != null) return String.valueOf(id);
        }
        String name = authentication.getName();
        if (name != null && !name.isBlank()) return name;
        return null;
    }
}
