package com.bt.accounts.service;

import com.bt.accounts.client.*;
import com.bt.accounts.config.PricingServiceProperties;
import com.bt.accounts.dto.*;
import com.bt.accounts.entity.FdAccount;
import com.bt.accounts.entity.AccountTransaction;
import com.bt.accounts.entity.CashCachedLedgerEntry;
import com.bt.accounts.exception.*;
import com.bt.accounts.event.*;
import com.bt.accounts.repository.FdAccountRepository;
import com.bt.accounts.repository.AccountTransactionRepository;
import com.bt.accounts.time.TimeProvider;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
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

    private static final ParameterizedTypeReference<ApiResponse<FetchedProductResponse>> PRODUCT_RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final FdAccountRepository accountRepository;
    private final AccountTransactionRepository transactionRepository;
    private final CashCachedService cashCachedService;
    private final PricingRuleEvaluator pricingRuleEvaluator;
    private final AccountNumberGenerator accountNumberGenerator;
    private final KafkaProducerService kafkaProducerService;
    private final RedisRequestResponseStore requestResponseStore;
    private final RestTemplate restTemplate;
    private final PricingServiceProperties pricingServiceProperties;
    private final ServiceTokenProvider serviceTokenProvider;

    @Value("${accounts.sequence.prefix:FD}")
    private String accountPrefix;

    @Value("${app.kafka.request-timeout-seconds:30}")
    private int requestTimeoutSeconds;

    @Transactional
    @CacheEvict(value = { "accounts", "customerAccounts" }, allEntries = true)
    public AccountResponse createAccount(AccountCreationRequest request, String authToken) {
        validateUserRole();

        validateCustomer(request.getCustomerId(), authToken);
        ProductDto product = fetchProduct(request.getProductCode(), authToken);

        validateProductRules(request, product);

        BigDecimal principalTokens = requireWholeTokens(request.getPrincipalAmount());
        if (principalTokens.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAccountDataException("Principal must be at least 1 CashCached token (1 KWD)");
        }

        FdCalculationDto calculation = calculateMaturity(request, authToken);
        BigDecimal maturityTokens = requireWholeTokens(calculation.getMaturityAmount());

        String accountNo = accountNumberGenerator.generateAccountNumber(request.getBranchCode());

        CashCachedLedgerEntry fundingEntry = fundAccountFromWallet(request.getCustomerId(), principalTokens,
                accountNo);
        log.info("Funded FD account {} from wallet. Ledger entry id={}, txHash={}", accountNo,
                fundingEntry != null ? fundingEntry.getId() : null,
                fundingEntry != null ? fundingEntry.getTransactionHash() : null);

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
                .prematurePenaltyRate(resolvePenaltyRate(product))
                .prematurePenaltyGraceDays(resolvePenaltyGraceDays(product))
                .build();

        FdAccount pricedAccount = applyInitialPricing(account, principalTokens, authToken);
        FdAccount savedAccount = accountRepository.save(pricedAccount);

        recordInitialDepositTransaction(savedAccount, principalTokens);
        recordContractLedgerEntry(request.getCustomerId(), principalTokens, accountNo);

        log.info("Created FD account: {} for customer: {}", accountNo, request.getCustomerId());

        return mapAccountResponse(savedAccount);
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(String accountNo) {
        FdAccount account = accountRepository.findByAccountNo(accountNo)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNo));
        return mapAccountResponse(account);
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

        ProductDto product = fetchProduct(newProductCode, authToken);

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
        account.setMaturityDate(TimeProvider.currentDateTime().plusMonths(newTenureMonths));
        account.setProductRefId(product.getId());
        account.setProductMaxTenureMonths(product.getMaxTermMonths());
        account.setPrematurePenaltyRate(resolvePenaltyRate(product));
        account.setPrematurePenaltyGraceDays(resolvePenaltyGraceDays(product));

        FdAccount saved = accountRepository.save(account);
        log.info("Upgraded FD account: {} by user: {}", accountNo, getCurrentUsername());
        return mapAccountResponse(saved);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "customerAccounts", key = "#customerId")
    public List<AccountResponse> getCustomerAccounts(String customerId) {
        List<FdAccount> accounts = accountRepository.findAllByCustomerIdOrderByCreatedAtDesc(customerId);
        return accounts.stream()
                .map(this::mapAccountResponse)
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
                .map(this::mapAccountResponse)
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
        account.setClosedAt(TimeProvider.currentDateTime());
        account.setClosedBy(getCurrentUsername());
        account.setClosureReason(request.getClosureReason());

        FdAccount savedAccount = accountRepository.save(account);
        log.info("Closed FD account: {} by user: {}", accountNo, getCurrentUsername());

        return mapAccountResponse(savedAccount);
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
        log.info("Reopened FD account: {} by user: {}", accountNo, getCurrentUsername());
        return mapAccountResponse(saved);
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
                account.setPricingRuleAppliedAt(TimeProvider.currentDateTime());
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
                .timestamp(TimeProvider.currentDateTime())
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

    private ProductDto fetchProduct(String productCode, String authToken) {
        ProductDto product = validateProduct(productCode, authToken);
        if (product.getPrematurePenaltyRate() != null && product.getPrematurePenaltyGraceDays() != null) {
            return product;
        }
        log.warn("Product {} missing penalty data from Kafka response. Attempting REST fallback", productCode);
        try {
            ProductDto refreshed = fetchProductDirect(productCode, authToken);
            if (refreshed.getPrematurePenaltyRate() != null
                    && refreshed.getPrematurePenaltyGraceDays() != null) {
                log.info("Product {} penalty refreshed via REST: rate={}, graceDays={}",
                        productCode, refreshed.getPrematurePenaltyRate(),
                        refreshed.getPrematurePenaltyGraceDays());
                return refreshed;
            }
            log.warn("Product {} REST fallback still missing penalty data", productCode);
        } catch (ServiceIntegrationException ex) {
            log.warn("Product {} REST fallback failed: {}", productCode, ex.getMessage());
        }
        return product;
    }

    private ProductDto fetchProductDirect(String productCode, String authToken) {
        String baseUrl = pricingServiceProperties.getUrl();
        if (!StringUtils.hasText(baseUrl)) {
            throw new ServiceIntegrationException("Pricing service base URL is not configured");
        }

        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/api/v1/product/{code}")
                .buildAndExpand(productCode)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(resolveBearerToken(authToken));

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<ApiResponse<FetchedProductResponse>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    PRODUCT_RESPONSE_TYPE);

            ApiResponse<FetchedProductResponse> body = response.getBody();
            if (body == null || !Boolean.TRUE.equals(body.getSuccess())) {
                String message = body != null ? body.getMessage() : "empty body";
                throw new ServiceIntegrationException(
                        "Product service REST call failed for code " + productCode + ": " + message);
            }

            FetchedProductResponse data = body.getData();
            if (data == null) {
                throw new ServiceIntegrationException(
                        "Product service REST call returned null data for code " + productCode);
            }

            return ProductDto.builder()
                    .id(data.getId())
                    .productCode(data.getProductCode())
                    .productName(data.getProductName())
                    .productType(data.getProductType())
                    .description(data.getDescription())
                    .minInterestRate(data.getMinInterestRate())
                    .maxInterestRate(data.getMaxInterestRate())
                    .minTermMonths(data.getMinTermMonths())
                    .maxTermMonths(data.getMaxTermMonths())
                    .minAmount(data.getMinAmount())
                    .maxAmount(data.getMaxAmount())
                    .currency(data.getCurrency())
                    .status(data.getStatus())
                    .prematurePenaltyRate(data.getPrematurePenaltyRate())
                    .prematurePenaltyGraceDays(data.getPrematurePenaltyGraceDays())
                    .build();
        } catch (RestClientException ex) {
            throw new ServiceIntegrationException("Failed to fetch product " + productCode + " via REST", ex);
        }
    }

    private String resolveBearerToken(String authToken) {
        if (StringUtils.hasText(authToken)) {
            String candidate = authToken.trim();
            return candidate.startsWith("Bearer ") ? candidate.substring(7) : candidate;
        }
        if (pricingServiceProperties.getToken() != null && StringUtils.hasText(pricingServiceProperties.getToken())) {
            String candidate = pricingServiceProperties.getToken().trim();
            return candidate.startsWith("Bearer ") ? candidate.substring(7) : candidate;
        }
        String serviceToken = serviceTokenProvider.getBearerToken();
        if (!StringUtils.hasText(serviceToken)) {
            throw new ServiceIntegrationException("Missing authorization token for product service");
        }
        return serviceToken.startsWith("Bearer ") ? serviceToken.substring(7) : serviceToken;
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
                    .prematurePenaltyRate(response.getPrematurePenaltyRate())
                    .prematurePenaltyGraceDays(response.getPrematurePenaltyGraceDays())
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

    private AccountResponse mapAccountResponse(FdAccount account) {
        AccountResponse response = AccountResponse.fromEntity(account);
        BigDecimal principal = account.getPrincipalAmount() != null ? account.getPrincipalAmount() : BigDecimal.ZERO;
        BigDecimal currentBalance = principal;
        try {
            BigDecimal latestBalance = computeCurrentBalance(account.getAccountNo());
            currentBalance = latestBalance;
        } catch (Exception ex) {
            log.warn("Unable to compute current balance for account {}: {}", account.getAccountNo(), ex.getMessage());
        }

        response.setCurrentBalance(currentBalance);
        BigDecimal accruedInterest = currentBalance.subtract(principal);
        if (accruedInterest.compareTo(BigDecimal.ZERO) < 0) {
            accruedInterest = BigDecimal.ZERO;
        }
        response.setAccruedInterest(accruedInterest);
        response.setPrematurePenaltyRate(account.getPrematurePenaltyRate());
        response.setPrematurePenaltyGraceDays(account.getPrematurePenaltyGraceDays());
        return response;
    }

    private void recordContractLedgerEntry(String customerId, BigDecimal amount, String accountNo) {
        try {
            cashCachedService.recordContractLock(customerId, amount, "FD Contract - " + accountNo);
        } catch (Exception ex) {
            log.warn("Failed to record contract ledger entry for account {}: {}", accountNo, ex.getMessage());
        }
    }

    private BigDecimal resolvePenaltyRate(ProductDto product) {
        return product != null && product.getPrematurePenaltyRate() != null
                ? product.getPrematurePenaltyRate()
                : BigDecimal.ZERO;
    }

    private Integer resolvePenaltyGraceDays(ProductDto product) {
        return product != null && product.getPrematurePenaltyGraceDays() != null
                ? product.getPrematurePenaltyGraceDays()
                : 0;
    }

    private static class FetchedProductResponse {
        private Long id;
        private String productCode;
        private String productName;
        private String productType;
        private String description;
        private BigDecimal minInterestRate;
        private BigDecimal maxInterestRate;
        private Integer minTermMonths;
        private Integer maxTermMonths;
        private BigDecimal minAmount;
        private BigDecimal maxAmount;
        private String currency;
        private String status;
        private BigDecimal prematurePenaltyRate;
        private Integer prematurePenaltyGraceDays;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getProductCode() {
            return productCode;
        }

        public void setProductCode(String productCode) {
            this.productCode = productCode;
        }

        public String getProductName() {
            return productName;
        }

        public void setProductName(String productName) {
            this.productName = productName;
        }

        public String getProductType() {
            return productType;
        }

        public void setProductType(String productType) {
            this.productType = productType;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public BigDecimal getMinInterestRate() {
            return minInterestRate;
        }

        public void setMinInterestRate(BigDecimal minInterestRate) {
            this.minInterestRate = minInterestRate;
        }

        public BigDecimal getMaxInterestRate() {
            return maxInterestRate;
        }

        public void setMaxInterestRate(BigDecimal maxInterestRate) {
            this.maxInterestRate = maxInterestRate;
        }

        public Integer getMinTermMonths() {
            return minTermMonths;
        }

        public void setMinTermMonths(Integer minTermMonths) {
            this.minTermMonths = minTermMonths;
        }

        public Integer getMaxTermMonths() {
            return maxTermMonths;
        }

        public void setMaxTermMonths(Integer maxTermMonths) {
            this.maxTermMonths = maxTermMonths;
        }

        public BigDecimal getMinAmount() {
            return minAmount;
        }

        public void setMinAmount(BigDecimal minAmount) {
            this.minAmount = minAmount;
        }

        public BigDecimal getMaxAmount() {
            return maxAmount;
        }

        public void setMaxAmount(BigDecimal maxAmount) {
            this.maxAmount = maxAmount;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public BigDecimal getPrematurePenaltyRate() {
            return prematurePenaltyRate;
        }

        public void setPrematurePenaltyRate(BigDecimal prematurePenaltyRate) {
            this.prematurePenaltyRate = prematurePenaltyRate;
        }

        public Integer getPrematurePenaltyGraceDays() {
            return prematurePenaltyGraceDays;
        }

        public void setPrematurePenaltyGraceDays(Integer prematurePenaltyGraceDays) {
            this.prematurePenaltyGraceDays = prematurePenaltyGraceDays;
        }
    }

    @Transactional
    @CacheEvict(value = { "accounts", "customerAccounts" }, allEntries = true)
    public AccountResponse createAccountV1(AccountCreationV1Request request, String authToken) {
        validateUserRole();

        validateCustomer(request.getCustomerId(), authToken);

        ProductDto product = fetchProduct(request.getProductCode(), authToken);

        log.info("[V1] Product {} penalty config: rate={}, graceDays={}",
                product.getProductCode(),
                product.getPrematurePenaltyRate(),
                product.getPrematurePenaltyGraceDays());

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
                .prematurePenaltyRate(resolvePenaltyRate(product))
                .prematurePenaltyGraceDays(resolvePenaltyGraceDays(product))
                .build();

        log.info("[V1] Creating FD account {} with penalty rate {} and graceDays {}",
                accountNo,
                account.getPrematurePenaltyRate(),
                account.getPrematurePenaltyGraceDays());

        FdAccount pricedAccount = applyInitialPricing(account, principalTokens, authToken);
        FdAccount savedAccount = accountRepository.save(pricedAccount);

        log.info("[V1] Saved FD account {} penalty persisted as rate={} graceDays={}",
                savedAccount.getAccountNo(),
                savedAccount.getPrematurePenaltyRate(),
                savedAccount.getPrematurePenaltyGraceDays());

        recordInitialDepositTransaction(savedAccount, principalTokens);
        recordContractLedgerEntry(request.getCustomerId(), principalTokens, accountNo);
        log.info("Created V1 FD account (product defaults): {} for customer: {}", accountNo, request.getCustomerId());

        return mapAccountResponse(savedAccount);
    }

    @Transactional
    @CacheEvict(value = { "accounts", "customerAccounts" }, allEntries = true)
    public AccountResponse createAccountV2(AccountCreationV2Request request, String authToken) {
        validateUserRole();

        validateCustomer(request.getCustomerId(), authToken);
        ProductDto product = fetchProduct(request.getProductCode(), authToken);

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
                .prematurePenaltyRate(resolvePenaltyRate(product))
                .prematurePenaltyGraceDays(resolvePenaltyGraceDays(product))
                .build();

        if (log.isDebugEnabled()) {
            log.debug("[V2] Creating FD account {} with penalty rate {} and graceDays {}",
                    accountNo,
                    account.getPrematurePenaltyRate(),
                    account.getPrematurePenaltyGraceDays());
        }

        FdAccount savedAccount = accountRepository.save(account);

        if (log.isDebugEnabled()) {
            log.debug("[V2] Saved FD account {} penalty persisted as rate={} graceDays={}",
                    savedAccount.getAccountNo(),
                    savedAccount.getPrematurePenaltyRate(),
                    savedAccount.getPrematurePenaltyGraceDays());
        }

        recordInitialDepositTransaction(savedAccount, principalTokens);
        recordContractLedgerEntry(request.getCustomerId(), principalTokens, accountNo);

        log.info("Created V2 FD account (custom values): {} for customer: {} with rate: {}%, tenure: {} months",
                accountNo, request.getCustomerId(), finalInterestRate, finalTenure);

        return mapAccountResponse(savedAccount);
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
                    .transactionDate(TimeProvider.currentDateTime())
                    .build();
            transactionRepository.save(transaction);
            log.info("Recorded initial deposit transaction: {} for account: {}", transactionId, account.getAccountNo());
        } catch (Exception e) {
            log.error("Failed to record initial deposit transaction for account: {}", account.getAccountNo(), e);
        }
    }

    private CashCachedLedgerEntry fundAccountFromWallet(String customerId, BigDecimal principalTokens,
            String accountNo) {
        try {
            CashCachedRedeemRequest request = new CashCachedRedeemRequest();
            request.setCustomerId(customerId);
            request.setAmount(principalTokens);
            request.setReference("FD Funding - " + accountNo);
            return cashCachedService.redeem(request);
        } catch (InvalidAccountDataException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            log.error("Wallet funding failed for account {}: {}", accountNo, ex.getMessage());
            throw new ServiceIntegrationException("Unable to fund FD account from wallet", ex);
        }
    }
}