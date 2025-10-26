package com.bt.accounts.service;

import com.bt.accounts.client.*;
import com.bt.accounts.dto.*;
import com.bt.accounts.entity.FdAccount;
import com.bt.accounts.exception.*;
import com.bt.accounts.repository.FdAccountRepository;
import com.bt.accounts.repository.AccountTransactionRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final FdAccountRepository accountRepository;
    private final AccountTransactionRepository transactionRepository;
    private final CustomerServiceClient customerServiceClient;
    private final ProductServiceClient productServiceClient;
    private final FdCalculatorServiceClient fdCalculatorServiceClient;
    private final AccountNumberGenerator accountNumberGenerator;

    @Value("${accounts.sequence.prefix:FD}")
    private String accountPrefix;

    @Transactional
    public AccountResponse createAccount(AccountCreationRequest request, String authToken) {
        validateUserRole();

        CustomerDto customer = validateCustomer(request.getCustomerId(), authToken);
        ProductDto product = validateProduct(request.getProductCode(), authToken);

        validateProductRules(request, product);

        FdCalculationDto calculation = calculateMaturity(request, authToken);

        String accountNo = accountNumberGenerator.generateAccountNumber(request.getBranchCode());

        FdAccount account = FdAccount.builder()
                .accountNo(accountNo)
                .customerId(request.getCustomerId())
                .productCode(request.getProductCode())
                .principalAmount(request.getPrincipalAmount())
                .interestRate(request.getInterestRate())
                .tenureMonths(request.getTenureMonths())
                .maturityAmount(calculation.getMaturityAmount())
                .maturityDate(LocalDateTime.now().plusMonths(request.getTenureMonths()))
                .branchCode(request.getBranchCode())
                .status(FdAccount.AccountStatus.ACTIVE)
                .createdBy(getCurrentUsername())
                .build();

        FdAccount savedAccount = accountRepository.save(account);
        log.info("Created FD account: {} for customer: {}", accountNo, request.getCustomerId());

        return AccountResponse.fromEntity(savedAccount);
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

        String newProductCode = request.get("productCode") != null ? String.valueOf(request.get("productCode")) : account.getProductCode();
        BigDecimal newInterestRate = request.get("interestRate") != null ? new BigDecimal(String.valueOf(request.get("interestRate"))) : account.getInterestRate();
        Integer newTenureMonths = request.get("tenureMonths") != null ? Integer.valueOf(String.valueOf(request.get("tenureMonths"))) : account.getTenureMonths();
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

        FdCalculationDto calc = calculateMaturity(temp, authToken);

        account.setProductCode(newProductCode);
        account.setInterestRate(newInterestRate);
        account.setTenureMonths(newTenureMonths);
        account.setPrincipalAmount(newPrincipal);
        account.setMaturityAmount(calc.getMaturityAmount());
        account.setMaturityDate(LocalDateTime.now().plusMonths(newTenureMonths));

        FdAccount saved = accountRepository.save(account);
        AccountResponse resp = AccountResponse.fromEntity(saved);
        resp.setCurrentBalance(computeCurrentBalance(accountNo));
        log.info("Upgraded FD account: {} by user: {}", accountNo, getCurrentUsername());
        return resp;
    }

    @Transactional(readOnly = true)
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

    @Transactional
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

    private CustomerDto validateCustomer(String customerId, String authToken) {
        try {
            ApiResponse<CustomerDto> response = customerServiceClient.getCustomerById(customerId, authToken);
            if (response.getData() == null) {
                throw new CustomerNotFoundException("Customer not found: " + customerId);
            }
            return response.getData();
        } catch (FeignException e) {
            log.error("Failed to validate customer: {}", customerId, e);
            throw new ServiceIntegrationException("Failed to validate customer with Customer Service", e);
        }
    }

    private ProductDto validateProduct(String productCode, String authToken) {
        try {
            ApiResponse<ProductDto> response = productServiceClient.getProductByCode(productCode, authToken);
            if (response.getData() == null) {
                throw new ProductNotFoundException("Product not found: " + productCode);
            }
            return response.getData();
        } catch (FeignException e) {
            log.error("Failed to validate product: {}", productCode, e);
            throw new ServiceIntegrationException("Failed to validate product with Product Service", e);
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
        try {
            Map<String, Object> calculationRequest = new HashMap<>();
            calculationRequest.put("customerId", request.getCustomerId());
            calculationRequest.put("productCode", request.getProductCode());
            calculationRequest.put("principalAmount", request.getPrincipalAmount());
            calculationRequest.put("interestRate", request.getInterestRate());
            calculationRequest.put("tenureMonths", request.getTenureMonths());

            ApiResponse<FdCalculationDto> response = fdCalculatorServiceClient.calculateFd(
                    calculationRequest, authToken);

            if (response.getData() == null) {
                throw new ServiceIntegrationException("Failed to calculate FD maturity");
            }

            return response.getData();
        } catch (FeignException e) {
            log.error("Failed to calculate maturity for account", e);
            throw new ServiceIntegrationException("Failed to calculate maturity with FD Calculator Service", e);
        }
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : "system";
    }

    private java.math.BigDecimal computeCurrentBalance(String accountNo) {
        List<com.bt.accounts.entity.AccountTransaction> txns = transactionRepository.findByAccountNoOrderByTransactionDateDesc(accountNo);
        if (txns == null || txns.isEmpty()) {
            FdAccount account = accountRepository.findByAccountNo(accountNo).orElseThrow();
            java.math.BigDecimal pa = account.getPrincipalAmount();
            return pa != null ? pa : java.math.BigDecimal.ZERO;
        }
        return txns.get(0).getBalanceAfter();
    }
}
