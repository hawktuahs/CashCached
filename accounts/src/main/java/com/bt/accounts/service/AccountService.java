package com.bt.accounts.service;

import com.bt.accounts.client.*;
import com.bt.accounts.dto.*;
import com.bt.accounts.entity.FdAccount;
import com.bt.accounts.entity.AccountTransaction;
import com.bt.accounts.exception.*;
import com.bt.accounts.event.*;
import com.bt.accounts.repository.FdAccountRepository;
import com.bt.accounts.repository.AccountTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final FdAccountRepository accountRepository;
    private final AccountTransactionRepository transactionRepository;
    private final CashCachedService cashCachedService;
    private final PricingRuleEvaluator pricingRuleEvaluator;
    private final AccountNumberGenerator accountNumberGenerator;
    private final KafkaProducerService kafkaProducerService;
    private final RedisRequestResponseStore requestResponseStore;

    @Value("${accounts.sequence.prefix:FD}")
    private String accountPrefix;

    @Value("${app.kafka.request-timeout-seconds:30}")
    private int requestTimeoutSeconds;

    @Transactional
    @CacheEvict(value = { "accounts", "customerAccounts" }, allEntries = true)
    public AccountResponse createAccount(AccountCreationRequest request, String authToken) {
        validateUserRole();

        validateCustomer(request.getCustomerId(), authToken);
        ProductDto product = validateProduct(request.getProductCode(), authToken);

        validateProductRules(request, product);

        BigDecimal principalTokens = requireWholeTokens(request.getPrincipalAmount());
        if (principalTokens.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAccountDataException("Principal must be at least 1 CashCached token (1 KWD)");
        }

        FdCalculationDto calculation = calculateMaturity(request, authToken);
        BigDecimal maturityTokens = requireWholeTokens(calculation.getMaturityAmount());

        String accountNo = accountNumberGenerator.generateAccountNumber(request.getBranchCode());

        FdAccount account = FdAccount.builder()
                .accountNo(accountNo)
                .customerId(request.getCustomerId())
                .productCode(request.getProductCode())
                .productRefId(product.getId())
                .productType(product.getProductType())
                .principalAmount(principalTokens)
                .interestRate(request.getInterestRate())
                .baseInterestRate(request.getInterestRate())
                .tenureMonths(request.getTenureMonths())
                .productMaxTenureMonths(product.getMaxTermMonths())
                .maturityAmount(maturityTokens)
                .branchCode(request.getBranchCode())
                .status(FdAccount.AccountStatus.ACTIVE)
                .createdBy(getCurrentUsername())
                .build();

        FdAccount pricedAccount = applyInitialPricing(account, principalTokens, authToken);
        FdAccount savedAccount = accountRepository.save(pricedAccount);

        recordInitialDepositTransaction(savedAccount, principalTokens);

        log.info("Created FD account: {} for customer: {}", accountNo, request.getCustomerId());

        AccountResponse response = AccountResponse.fromEntity(savedAccount);
        response.setCurrentBalance(savedAccount.getPrincipalAmount());
        return response;
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(String accountNo) {
        FdAccount account = accountRepository.findByAccountNo(accountNo)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNo));
        AccountResponse resp = AccountResponse.fromEntity(account);
        resp.setCurrentBalance(computeCurrentBalance(accountNo));
        return resp;
    }

    @Transactional
    public AccountResponse upgradeAccount(String accountNo, Map<String, Object> request, String authToken) {
        validateUserRole();

        FdAccount account = accountRepository.findByAccountNo(accountNo)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNo));

        if (account.getStatus() == FdAccount.AccountStatus.CLOSED) {
            throw new InvalidAccountDataException("Cannot upgrade a closed account: " + accountNo);
        }

        String newProductCode = request.get("productCode") != null ? String.valueOf(request.get("productCode"))
                : account.getProductCode();
        BigDecimal newInterestRate = request.get("interestRate") != null
                ? new BigDecimal(String.valueOf(request.get("interestRate")))
                : account.getInterestRate();
        Integer newTenureMonths = request.get("tenureMonths") != null
                ? Integer.valueOf(String.valueOf(request.get("tenureMonths")))
                : account.getTenureMonths();
        BigDecimal newPrincipal = account.getPrincipalAmount();
        if (request.get("principalAmount") != null) {
            newPrincipal = new BigDecimal(String.valueOf(request.get("principalAmount")));
        }

        ProductDto product = validateProduct(newProductCode, authToken);

        AccountCreationRequest temp = AccountCreationRequest.builder()
                .customerId(account.getCustomerId())
                .productCode(newProductCode)
                .principalAmount(newPrincipal)
                .interestRate(newInterestRate)
                .tenureMonths(newTenureMonths)
                .branchCode(account.getBranchCode())
                .build();

        validateProductRules(temp, product);

        BigDecimal newPrincipalTokens = requireWholeTokens(newPrincipal);
        if (newPrincipalTokens.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAccountDataException("Principal must be at least 1 CashCached token (1 KWD)");
        }

        FdCalculationDto calc = calculateMaturity(temp, authToken);

        account.setProductCode(newProductCode);
        account.setProductType(product.getProductType());
        account.setInterestRate(newInterestRate);
        account.setBaseInterestRate(newInterestRate);
        account.setTenureMonths(newTenureMonths);
        account.setPrincipalAmount(newPrincipalTokens);
        account.setMaturityAmount(calc.getMaturityAmount());
        account.setMaturityDate(LocalDateTime.now().plusMonths(newTenureMonths));
        account.setProductRefId(product.getId());
        account.setProductMaxTenureMonths(product.getMaxTermMonths());

        FdAccount saved = accountRepository.save(account);
        AccountResponse resp = AccountResponse.fromEntity(saved);
        resp.setCurrentBalance(computeCurrentBalance(accountNo));
        log.info("Upgraded FD account: {} by user: {}", accountNo, getCurrentUsername());
        return resp;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "customerAccounts", key = "#customerId")
    public List<AccountResponse> getCustomerAccounts(String customerId) {
        List<FdAccount> accounts = accountRepository.findAllByCustomerIdOrderByCreatedAtDesc(customerId);
        return accounts.stream()
                .map(a -> {
                    AccountResponse r = AccountResponse.fromEntity(a);
                    r.setCurrentBalance(computeCurrentBalance(a.getAccountNo()));
                    return r;
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PagedResponse<AccountResponse> searchAccounts(AccountSearchRequest searchRequest) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(
                searchRequest.getPage(),
                searchRequest.getSize(),
                org.springframework.data.domain.Sort.Direction.fromString(searchRequest.getSortDirection()),
                searchRequest.getSortBy());

        org.springframework.data.domain.Page<FdAccount> page;

        if (searchRequest.getCustomerId() != null || searchRequest.getProductCode() != null ||
                searchRequest.getStatus() != null || searchRequest.getBranchCode() != null) {
            FdAccount.AccountStatus status = null;
            if (searchRequest.getStatus() != null) {
                status = FdAccount.AccountStatus.valueOf(searchRequest.getStatus());
            }
            page = accountRepository.searchAccounts(
                    searchRequest.getCustomerId(),
                    searchRequest.getProductCode(),
                    status,
                    searchRequest.getBranchCode(),
                    pageable);
        } else {
            page = accountRepository.findAll(pageable);
        }

        List<AccountResponse> content = page.getContent().stream()
                .map(a -> {
                    AccountResponse r = AccountResponse.fromEntity(a);
                    r.setCurrentBalance(computeCurrentBalance(a.getAccountNo()));
                    return r;
                })
                .collect(Collectors.toList());

        return PagedResponse.<AccountResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    @Transactional
    @CacheEvict(value = { "accounts", "customerAccounts" }, allEntries = true)
    public AccountResponse closeAccount(String accountNo, AccountClosureRequest request) {
        validateUserRole();

        FdAccount account = accountRepository.findByAccountNo(accountNo)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNo));

        if (account.getStatus() == FdAccount.AccountStatus.CLOSED) {
            throw new AccountAlreadyClosedException("Account is already closed: " + accountNo);
        }

        account.setStatus(FdAccount.AccountStatus.CLOSED);
        account.setClosedAt(LocalDateTime.now());
        account.setClosedBy(getCurrentUsername());
        account.setClosureReason(request.getClosureReason());

        FdAccount savedAccount = accountRepository.save(account);
        log.info("Closed FD account: {} by user: {}", accountNo, getCurrentUsername());

        return AccountResponse.fromEntity(savedAccount);
    }

    @Transactional
    public AccountResponse reopenAccount(String accountNo) {
        validateUserRole();

        FdAccount account = accountRepository.findByAccountNo(accountNo)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNo));

        if (account.getStatus() != FdAccount.AccountStatus.CLOSED) {
            throw new InvalidAccountDataException("Account is not closed: " + accountNo);
        }

        account.setStatus(FdAccount.AccountStatus.ACTIVE);
        account.setClosedAt(null);
        account.setClosedBy(null);
        account.setClosureReason(null);

        FdAccount saved = accountRepository.save(account);
        AccountResponse resp = AccountResponse.fromEntity(saved);
        resp.setCurrentBalance(computeCurrentBalance(accountNo));
        log.info("Reopened FD account: {} by user: {}", accountNo, getCurrentUsername());
        return resp;
    }

    private void validateUserRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedAccessException("User not authenticated");
        }

        boolean hasRequiredRole = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_BANKOFFICER") ||
                        auth.getAuthority().equals("ROLE_ADMIN"));

        if (!hasRequiredRole) {
            throw new UnauthorizedAccessException("User does not have required role (BANKOFFICER or ADMIN)");
        }
    }

    private FdAccount applyInitialPricing(FdAccount account, BigDecimal principalTokens, String authToken) {
        if (account == null) {
            return null;
        }

        BigDecimal balance = principalTokens != null ? principalTokens : account.getPrincipalAmount();
        if (balance == null) {
            balance = BigDecimal.ZERO;
        }

        try {
            PricingRuleEvaluator.EvaluationResult evaluation = pricingRuleEvaluator.evaluate(account, balance,
                    authToken);
            if (evaluation.hasRule()) {
                account.setActivePricingRuleId(evaluation.getRule().getId());
                account.setActivePricingRuleName(evaluation.getRule().getRuleName());
                account.setPricingRuleAppliedAt(LocalDateTime.now());
            }
            if (evaluation.getAppliedRate() != null) {
                account.setInterestRate(evaluation.getAppliedRate());
            }
            return account;
        } catch (ServiceIntegrationException ex) {
            log.warn("Initial pricing evaluation failed for account {}: {}", account.getAccountNo(), ex.getMessage());
            return account;
        }
    }

    private CustomerDto validateCustomer(String customerId, String authToken) {
        String requestId = UUID.randomUUID().toString();
        log.info("========== VALIDATING CUSTOMER {} ==========", customerId);
        log.info("Generated requestId: {}", requestId);

        CustomerValidationRequest request = CustomerValidationRequest.builder()
                .customerId(Long.parseLong(customerId))
                .requestId(requestId)
                .timestamp(LocalDateTime.now())
                .build();

        log.info("Storing pending request with requestId: {}", requestId);
        requestResponseStore.putRequest(requestId, null);

        log.info("Sending customer validation request to Kafka...");
        kafkaProducerService.sendCustomerValidationRequest(request);
        log.info("Request sent. Now waiting for response (timeout: {} seconds)...", requestTimeoutSeconds);

        try {
            CustomerValidationResponse response = requestResponseStore
                    .getResponse(requestId, CustomerValidationResponse.class, requestTimeoutSeconds, TimeUnit.SECONDS);

            log.info("========== RESPONSE RECEIVED ==========");
            log.info("Response: {}", response != null ? "NOT NULL" : "NULL");

            if (response == null || !Boolean.TRUE.equals(response.getValid())) {
                log.error("Customer validation FAILED - response null or invalid");
                throw new CustomerNotFoundException("Customer not found or invalid: " + customerId);
            }

            log.info("Customer validation SUCCESSFUL");
            CustomerDto dto = new CustomerDto();
            dto.setId(response.getCustomerId());
            return dto;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Customer validation INTERRUPTED");
            throw new ServiceIntegrationException("Customer validation request timeout", e);
        }
    }

    private ProductDto validateProduct(String productCode, String authToken) {
        String requestId = UUID.randomUUID().toString();
        ProductDetailsRequest request = ProductDetailsRequest.builder()
                .productCode(productCode)
                .requestId(requestId)
                .timestamp(LocalDateTime.now())
                .build();

        requestResponseStore.putRequest(requestId, null);
        kafkaProducerService.sendProductDetailsRequest(request);

        try {
            ProductDetailsResponse response = requestResponseStore
                    .getResponse(requestId, ProductDetailsResponse.class, requestTimeoutSeconds, TimeUnit.SECONDS);

            if (response == null) {
                throw new ServiceIntegrationException("Product details not found for code: " + productCode);
            }
            if (response.getError() != null && !response.getError().isBlank()) {
                throw new ServiceIntegrationException(
                        "Product service returned error for code " + productCode + ": " + response.getError());
            }

            ProductDto dto = ProductDto.builder()
                    .id(response.getProductId())
                    .productCode(response.getProductCode())
                    .productName(response.getProductName())
                    .productType(response.getProductType())
                    .minAmount(response.getMinAmount())
                    .maxAmount(response.getMaxAmount())
                    .minTermMonths(response.getMinTermMonths())
                    .maxTermMonths(response.getMaxTermMonths())
                    .minInterestRate(response.getMinInterestRate())
                    .maxInterestRate(response.getMaxInterestRate())
                    .currency(response.getCurrency())
                    .status(response.getStatus())
                    .build();
            return dto;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceIntegrationException("Product details request timeout", e);
        }
    }

    private void validateProductRules(AccountCreationRequest request, ProductDto product) {
        if (request.getPrincipalAmount().compareTo(product.getMinAmount()) < 0) {
            throw new InvalidAccountDataException(
                    String.format("Principal amount %.2f is below minimum %.2f for product %s",
                            request.getPrincipalAmount(), product.getMinAmount(), product.getProductCode()));
        }

        if (request.getPrincipalAmount().compareTo(product.getMaxAmount()) > 0) {
            throw new InvalidAccountDataException(
                    String.format("Principal amount %.2f exceeds maximum %.2f for product %s",
                            request.getPrincipalAmount(), product.getMaxAmount(), product.getProductCode()));
        }

        if (request.getTenureMonths() < product.getMinTermMonths()) {
            throw new InvalidAccountDataException(
                    String.format("Tenure %d months is below minimum %d months for product %s",
                            request.getTenureMonths(), product.getMinTermMonths(), product.getProductCode()));
        }

        if (request.getTenureMonths() > product.getMaxTermMonths()) {
            throw new InvalidAccountDataException(
                    String.format("Tenure %d months exceeds maximum %d months for product %s",
                            request.getTenureMonths(), product.getMaxTermMonths(), product.getProductCode()));
        }

        if (request.getInterestRate().compareTo(product.getMinInterestRate()) < 0 ||
                request.getInterestRate().compareTo(product.getMaxInterestRate()) > 0) {
            throw new InvalidAccountDataException(
                    String.format("Interest rate %.2f%% is outside allowed range %.2f%% - %.2f%% for product %s",
                            request.getInterestRate(), product.getMinInterestRate(),
                            product.getMaxInterestRate(), product.getProductCode()));
        }
    }

    private FdCalculationDto calculateMaturity(AccountCreationRequest request, String authToken) {
        String requestId = UUID.randomUUID().toString();
        FdCalculationRequestEvent event = FdCalculationRequestEvent.builder()
                .customerId(Long.parseLong(request.getCustomerId()))
                .productCode(request.getProductCode())
                .principalAmount(request.getPrincipalAmount())
                .tenureMonths(request.getTenureMonths())
                .requestId(requestId)
                .timestamp(LocalDateTime.now())
                .build();

        requestResponseStore.putRequest(requestId, null);
        kafkaProducerService.sendFdCalculationRequest(event);

        try {
            FdCalculationResponseEvent response = requestResponseStore
                    .getResponse(requestId, FdCalculationResponseEvent.class, requestTimeoutSeconds, TimeUnit.SECONDS);

            if (response == null || response.getMaturityAmount() == null) {
                throw new ServiceIntegrationException("Failed to calculate FD maturity");
            }

            FdCalculationDto dto = new FdCalculationDto();
            dto.setMaturityAmount(response.getMaturityAmount());
            dto.setInterestEarned(response.getInterestEarned());
            dto.setEffectiveRate(response.getEffectiveRate());
            return dto;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceIntegrationException("FD calculation request timeout", e);
        }
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : "system";
    }

    private BigDecimal requireWholeTokens(BigDecimal amount) {
        if (amount == null) {
            throw new InvalidAccountDataException("Token amount is required");
        }
        try {
            BigDecimal normalized = amount.stripTrailingZeros();
            BigDecimal tokens = normalized.setScale(0, RoundingMode.DOWN);
            if (tokens.compareTo(BigDecimal.ONE) < 0) {
                throw new InvalidAccountDataException("Amount must be at least 1 CashCached token (1 KWD)");
            }
            return tokens;
        } catch (ArithmeticException ex) {
            throw new InvalidAccountDataException("CashCached tokens must be whole numbers");
        }
    }

    private BigDecimal computeCurrentBalance(String accountNo) {
        List<com.bt.accounts.entity.AccountTransaction> txns = transactionRepository
                .findByAccountNoOrderByTransactionDateDesc(accountNo);
        if (txns == null || txns.isEmpty()) {
            FdAccount account = accountRepository.findByAccountNo(accountNo).orElseThrow();
            BigDecimal pa = account.getPrincipalAmount();
            return pa != null ? pa : BigDecimal.ZERO;
        }
        return txns.get(0).getBalanceAfter();
    }

    @Transactional
    @CacheEvict(value = { "accounts", "customerAccounts" }, allEntries = true)
    public AccountResponse createAccountV1(AccountCreationV1Request request, String authToken) {
        validateUserRole();

        validateCustomer(request.getCustomerId(), authToken);
        ProductDto product = validateProduct(request.getProductCode(), authToken);

        BigDecimal principalTokens = requireWholeTokens(request.getPrincipalAmount());
        if (principalTokens.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAccountDataException("Principal must be at least 1 CashCached token (1 KWD)");
        }

        AccountCreationRequest fullRequest = AccountCreationRequest.builder()
                .customerId(request.getCustomerId())
                .productCode(request.getProductCode())
                .principalAmount(principalTokens)
                .interestRate(product.getMinInterestRate())
                .tenureMonths(product.getMinTermMonths())
                .branchCode(request.getBranchCode())
                .remarks(request.getRemarks())
                .build();

        validateProductRules(fullRequest, product);

        FdCalculationDto calculation = calculateMaturity(fullRequest, authToken);
        BigDecimal maturityTokens = requireWholeTokens(calculation.getMaturityAmount());

        String accountNo = accountNumberGenerator.generateAccountNumber(request.getBranchCode());

        FdAccount account = FdAccount.builder()
                .accountNo(accountNo)
                .customerId(request.getCustomerId())
                .productCode(request.getProductCode())
                .productRefId(product.getId())
                .productType(product.getProductType())
                .principalAmount(principalTokens)
                .interestRate(product.getMinInterestRate())
                .baseInterestRate(product.getMinInterestRate())
                .tenureMonths(product.getMinTermMonths())
                .productMaxTenureMonths(product.getMaxTermMonths())
                .maturityAmount(maturityTokens)
                .branchCode(request.getBranchCode())
                .status(FdAccount.AccountStatus.ACTIVE)
                .createdBy(getCurrentUsername())
                .build();

        FdAccount savedAccount = accountRepository.save(account);
        log.info("Created V1 FD account (product defaults): {} for customer: {}", accountNo, request.getCustomerId());

        return AccountResponse.fromEntity(savedAccount);
    }

    @Transactional
    @CacheEvict(value = { "accounts", "customerAccounts" }, allEntries = true)
    public AccountResponse createAccountV2(AccountCreationV2Request request, String authToken) {
        validateUserRole();

        validateCustomer(request.getCustomerId(), authToken);
        ProductDto product = validateProduct(request.getProductCode(), authToken);

        BigDecimal principalTokens = requireWholeTokens(request.getPrincipalAmount());
        if (principalTokens.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAccountDataException("Principal must be at least 1 CashCached token (1 KWD)");
        }

        BigDecimal finalInterestRate = request.getCustomInterestRate() != null
                ? request.getCustomInterestRate()
                : product.getMinInterestRate();
        Integer finalTenure = request.getCustomTenureMonths() != null
                ? request.getCustomTenureMonths()
                : product.getMinTermMonths();

        if (finalInterestRate.compareTo(product.getMinInterestRate()) < 0 ||
                finalInterestRate.compareTo(product.getMaxInterestRate()) > 0) {
            throw new InvalidAccountDataException(
                    String.format("Interest rate %.2f%% is outside product range %.2f%% - %.2f%%",
                            finalInterestRate, product.getMinInterestRate(), product.getMaxInterestRate()));
        }

        if (finalTenure < product.getMinTermMonths() || finalTenure > product.getMaxTermMonths()) {
            throw new InvalidAccountDataException(
                    String.format("Tenure %d months is outside product range %d - %d months",
                            finalTenure, product.getMinTermMonths(), product.getMaxTermMonths()));
        }

        AccountCreationRequest fullRequest = AccountCreationRequest.builder()
                .customerId(request.getCustomerId())
                .productCode(request.getProductCode())
                .principalAmount(principalTokens)
                .interestRate(finalInterestRate)
                .tenureMonths(finalTenure)
                .branchCode(request.getBranchCode())
                .remarks(request.getRemarks())
                .build();

        validateProductRules(fullRequest, product);

        FdCalculationDto calculation = calculateMaturity(fullRequest, authToken);
        BigDecimal maturityTokens = requireWholeTokens(calculation.getMaturityAmount());

        String accountNo = accountNumberGenerator.generateAccountNumber(request.getBranchCode());

        FdAccount account = FdAccount.builder()
                .accountNo(accountNo)
                .customerId(request.getCustomerId())
                .productCode(request.getProductCode())
                .productRefId(product.getId())
                .productType(product.getProductType())
                .principalAmount(principalTokens)
                .interestRate(finalInterestRate)
                .baseInterestRate(finalInterestRate)
                .tenureMonths(finalTenure)
                .productMaxTenureMonths(product.getMaxTermMonths())
                .maturityAmount(maturityTokens)
                .branchCode(request.getBranchCode())
                .status(FdAccount.AccountStatus.ACTIVE)
                .createdBy(getCurrentUsername())
                .build();

        FdAccount savedAccount = accountRepository.save(account);

        recordInitialDepositTransaction(savedAccount, principalTokens);

        log.info("Created V2 FD account (custom values): {} for customer: {} with rate: {}%, tenure: {} months",
                accountNo, request.getCustomerId(), finalInterestRate, finalTenure);

        return AccountResponse.fromEntity(savedAccount);
    }

    private void recordInitialDepositTransaction(FdAccount account, BigDecimal principalTokens) {
        try {
            String transactionId = UUID.randomUUID().toString();
            AccountTransaction transaction = AccountTransaction.builder()
                    .transactionId(transactionId)
                    .accountNo(account.getAccountNo())
                    .transactionType(AccountTransaction.TransactionType.DEPOSIT)
                    .amount(principalTokens)
                    .balanceAfter(principalTokens)
                    .description("Initial deposit for account creation")
                    .referenceNo("ACCOUNT_CREATION")
                    .processedBy(getCurrentUsername())
                    .transactionDate(LocalDateTime.now())
                    .build();
            transactionRepository.save(transaction);
            log.info("Recorded initial deposit transaction: {} for account: {}", transactionId, account.getAccountNo());
        } catch (Exception e) {
            log.error("Failed to record initial deposit transaction for account: {}", account.getAccountNo(), e);
        }
    }
}