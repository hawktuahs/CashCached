package com.bt.fixeddeposit.service;

import com.bt.fixeddeposit.client.CustomerServiceClient;
import com.bt.fixeddeposit.client.ProductServiceClient;
import com.bt.fixeddeposit.dto.FdCalculationRequest;
import com.bt.fixeddeposit.dto.FdCalculationResponse;
import com.bt.fixeddeposit.dto.external.CustomerResponse;
import com.bt.fixeddeposit.dto.external.ExternalApiResponse;
import com.bt.fixeddeposit.dto.external.ProductResponse;
import com.bt.fixeddeposit.entity.FdCalculation;
import com.bt.fixeddeposit.exception.*;
import com.bt.fixeddeposit.repository.FdCalculationRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FdCalculationService {

    private final FdCalculationRepository calculationRepository;
    private final CustomerServiceClient customerServiceClient;
    private final ProductServiceClient productServiceClient;

    @Value("${app.calculation.default-compounding-frequency}")
    private Integer defaultCompoundingFrequency;

    @Value("${app.calculation.rounding-scale}")
    private Integer roundingScale;

    @Transactional
    public FdCalculationResponse calculateFd(FdCalculationRequest request, String authToken) {
        log.info("Processing FD calculation request for customer: {} and product: {}",
                request.getCustomerId(), request.getProductCode());

        validateCustomer(request.getCustomerId(), authToken);
        ProductResponse product = fetchProductDetails(request.getProductCode(), authToken);
        validateCalculationRequest(request, product);

        Integer compoundingFrequency = request.getCompoundingFrequency() != null
                ? request.getCompoundingFrequency()
                : defaultCompoundingFrequency;

        BigDecimal interestRate = determineApplicableInterestRate(product, request);
        BigDecimal maturityAmount = calculateMaturityAmount(
                request.getPrincipalAmount(),
                interestRate,
                request.getTenureMonths(),
                compoundingFrequency);
        BigDecimal interestEarned = maturityAmount.subtract(request.getPrincipalAmount());
        BigDecimal effectiveRate = calculateEffectiveRate(interestRate, compoundingFrequency);

        FdCalculation calculation = FdCalculation.builder()
                .customerId(request.getCustomerId())
                .productCode(request.getProductCode())
                .principalAmount(request.getPrincipalAmount())
                .tenureMonths(request.getTenureMonths())
                .interestRate(interestRate)
                .compoundingFrequency(compoundingFrequency)
                .maturityAmount(maturityAmount)
                .interestEarned(interestEarned)
                .effectiveRate(effectiveRate)
                .currency(product.getCurrency())
                .build();

        FdCalculation savedCalculation = calculationRepository.save(calculation);
        log.info("FD calculation saved successfully with ID: {}", savedCalculation.getId());

        return buildCalculationResponse(savedCalculation, product.getProductName());
    }

    @Transactional(readOnly = true)
    public FdCalculationResponse getCalculationById(Long id, String authToken) {
        FdCalculation calculation = calculationRepository.findById(id)
                .orElseThrow(() -> new CalculationNotFoundException("Calculation not found with ID: " + id));

        ProductResponse product = fetchProductDetails(calculation.getProductCode(), authToken);
        return buildCalculationResponse(calculation, product.getProductName());
    }

    @Transactional(readOnly = true)
    public List<FdCalculationResponse> getCalculationHistory(Long customerId, String authToken) {
        validateCustomer(customerId, authToken);
        List<FdCalculation> calculations = calculationRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);

        return calculations.stream()
                .map(calc -> buildCalculationResponse(calc, fetchProductNameSafely(calc.getProductCode(), authToken)))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FdCalculationResponse> getRecentCalculations(Long customerId, Integer days, String authToken) {
        validateCustomer(customerId, authToken);
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        List<FdCalculation> calculations = calculationRepository.findRecentCalculationsByCustomer(customerId,
                startDate);

        return calculations.stream()
                .map(calc -> buildCalculationResponse(calc, fetchProductNameSafely(calc.getProductCode(), authToken)))
                .collect(Collectors.toList());
    }

    private void validateCustomer(Long customerId, String authToken) {
        try {
            ExternalApiResponse<CustomerResponse> response = customerServiceClient.getCustomerById(customerId,
                    authToken);
            if (response == null || !Boolean.TRUE.equals(response.getSuccess()) || response.getData() == null) {
                throw new CustomerNotFoundException("Customer not found with ID: " + customerId);
            }

            CustomerResponse customer = response.getData();
            if (!Boolean.TRUE.equals(customer.getActive())) {
                throw new InvalidCalculationDataException("Customer account is not active");
            }
        } catch (FeignException e) {
            log.error("Failed to validate customer with ID: {}", customerId, e);
            throw new ServiceIntegrationException("Failed to validate customer information", e);
        }
    }

    private ProductResponse fetchProductDetails(String productCode, String authToken) {
        try {
            ExternalApiResponse<ProductResponse> response = productServiceClient.getProductByCode(productCode,
                    authToken);
            if (response == null || !Boolean.TRUE.equals(response.getSuccess()) || response.getData() == null) {
                throw new ProductNotFoundException("Product not found with code: " + productCode);
            }

            ProductResponse product = response.getData();
            if (!"ACTIVE".equals(product.getStatus())) {
                throw new InvalidCalculationDataException("Product is not active: " + productCode);
            }

            return product;
        } catch (FeignException e) {
            log.error("Failed to fetch product details for code: {}", productCode, e);
            throw new ServiceIntegrationException("Failed to fetch product information", e);
        }
    }

    private String fetchProductNameSafely(String productCode, String authToken) {
        try {
            ProductResponse product = fetchProductDetails(productCode, authToken);
            return product.getProductName();
        } catch (Exception e) {
            log.warn("Failed to fetch product name for code: {}", productCode);
            return productCode;
        }
    }

    private void validateCalculationRequest(FdCalculationRequest request, ProductResponse product) {
        if (request.getPrincipalAmount().compareTo(product.getMinAmount()) < 0) {
            throw new InvalidCalculationDataException(
                    String.format("Principal amount must be at least %s", product.getMinAmount()));
        }

        if (request.getPrincipalAmount().compareTo(product.getMaxAmount()) > 0) {
            throw new InvalidCalculationDataException(
                    String.format("Principal amount cannot exceed %s", product.getMaxAmount()));
        }

        if (request.getTenureMonths() < product.getMinTermMonths()) {
            throw new InvalidCalculationDataException(
                    String.format("Tenure must be at least %d months", product.getMinTermMonths()));
        }

        if (request.getTenureMonths() > product.getMaxTermMonths()) {
            throw new InvalidCalculationDataException(
                    String.format("Tenure cannot exceed %d months", product.getMaxTermMonths()));
        }
    }

    private BigDecimal determineApplicableInterestRate(ProductResponse product, FdCalculationRequest request) {
        BigDecimal baseRate = product.getMinInterestRate();
        BigDecimal maxRate = product.getMaxInterestRate();

        if (request.getTenureMonths() >= 60) {
            return maxRate;
        } else if (request.getTenureMonths() >= 36) {
            return baseRate.add(maxRate.subtract(baseRate).multiply(BigDecimal.valueOf(0.75)));
        } else if (request.getTenureMonths() >= 12) {
            return baseRate.add(maxRate.subtract(baseRate).multiply(BigDecimal.valueOf(0.50)));
        }

        return baseRate;
    }

    private BigDecimal calculateMaturityAmount(BigDecimal principal, BigDecimal annualRate,
            Integer tenureMonths, Integer compoundingFrequency) {
        double p = principal.doubleValue();
        double r = annualRate.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP).doubleValue();
        double n = compoundingFrequency.doubleValue();
        double t = tenureMonths.doubleValue() / 12.0;

        double maturity = p * Math.pow(1 + (r / n), n * t);

        return BigDecimal.valueOf(maturity).setScale(roundingScale, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateEffectiveRate(BigDecimal nominalRate, Integer compoundingFrequency) {
        double r = nominalRate.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP).doubleValue();
        double n = compoundingFrequency.doubleValue();

        double effectiveRate = (Math.pow(1 + (r / n), n) - 1) * 100;

        return BigDecimal.valueOf(effectiveRate).setScale(roundingScale, RoundingMode.HALF_UP);
    }

    private FdCalculationResponse buildCalculationResponse(FdCalculation calculation, String productName) {
        return FdCalculationResponse.builder()
                .id(calculation.getId())
                .customerId(calculation.getCustomerId())
                .productCode(calculation.getProductCode())
                .productName(productName)
                .principalAmount(calculation.getPrincipalAmount())
                .tenureMonths(calculation.getTenureMonths())
                .interestRate(calculation.getInterestRate())
                .compoundingFrequency(calculation.getCompoundingFrequency())
                .maturityAmount(calculation.getMaturityAmount())
                .interestEarned(calculation.getInterestEarned())
                .effectiveRate(calculation.getEffectiveRate())
                .currency(calculation.getCurrency())
                .calculationDate(calculation.getCalculationDate())
                .createdAt(calculation.getCreatedAt())
                .build();
    }
}
