package com.bt.fixeddeposit.service;

import com.bt.fixeddeposit.dto.FdCalculationRequest;
import com.bt.fixeddeposit.dto.FdCalculationResponse;
import com.bt.fixeddeposit.dto.external.ProductResponse;
import com.bt.fixeddeposit.entity.FdCalculation;
import com.bt.fixeddeposit.event.CustomerValidationRequest;
import com.bt.fixeddeposit.event.CustomerValidationResponse;
import com.bt.fixeddeposit.event.KafkaProducerService;
import com.bt.fixeddeposit.event.ProductDetailsRequest;
import com.bt.fixeddeposit.event.ProductDetailsResponse;
import com.bt.fixeddeposit.event.RedisRequestResponseStore;
import com.bt.fixeddeposit.exception.*;
import com.bt.fixeddeposit.repository.FdCalculationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FdCalculationService {

    private final FdCalculationRepository calculationRepository;
    private final KafkaProducerService kafkaProducerService;
    private final RedisRequestResponseStore requestResponseStore;
    private final RedisFdCacheService redisFdCacheService;

    @Value("${app.calculation.default-compounding-frequency}")
    private Integer defaultCompoundingFrequency;

    @Value("${app.calculation.rounding-scale}")
    private Integer roundingScale;

    @Value("${app.kafka.request-timeout-seconds:30}")
    private long requestTimeoutSeconds;

    @Transactional
    public FdCalculationResponse calculateFd(FdCalculationRequest request, String authToken) {
        log.info("Processing FD calculation request for customer: {} and product: {}",
                request.getCustomerId(), request.getProductCode());

        FdCalculationResponse cachedResult = redisFdCacheService.getCachedCalculation(
                request.getCustomerId(),
                request.getProductCode(),
                request.getTenureMonths(),
                request.getPrincipalAmount());

        if (cachedResult != null) {
            log.info("Returning cached FD calculation result");
            return cachedResult;
        }

        validateCustomer(request.getCustomerId());
        ProductResponse product = fetchProductDetails(request.getProductCode());
        validateCalculationRequest(request, product);

        Integer compoundingFrequency = resolveCompoundingFrequency(request, product);

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

        FdCalculationResponse response = buildCalculationResponse(savedCalculation, product.getProductName());

        redisFdCacheService.cacheCalculation(
                request.getCustomerId(),
                request.getProductCode(),
                request.getTenureMonths(),
                request.getPrincipalAmount(),
                response);

        return response;
    }

    @Transactional(readOnly = true)
    public FdCalculationResponse getCalculationById(Long id, String authToken) {
        FdCalculation calculation = calculationRepository.findById(id)
                .orElseThrow(() -> new CalculationNotFoundException("Calculation not found with ID: " + id));

        ProductResponse product = fetchProductDetails(calculation.getProductCode());
        return buildCalculationResponse(calculation, product.getProductName());
    }

    @Transactional(readOnly = true)
    public List<FdCalculationResponse> getCalculationHistory(Long customerId, String authToken) {
        validateCustomer(customerId);
        List<FdCalculation> calculations = calculationRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);

        return calculations.stream()
                .map(calc -> buildCalculationResponse(calc, fetchProductNameSafely(calc.getProductCode())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FdCalculationResponse> getRecentCalculations(Long customerId, Integer days, String authToken) {
        validateCustomer(customerId);
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        List<FdCalculation> calculations = calculationRepository.findRecentCalculationsByCustomer(customerId,
                startDate);

        return calculations.stream()
                .map(calc -> buildCalculationResponse(calc, fetchProductNameSafely(calc.getProductCode())))
                .collect(Collectors.toList());
    }

    private void validateCustomer(Long customerId) {
        try {
            log.info("============ VALIDATING CUSTOMER {} ============", customerId);
            String requestId = UUID.randomUUID().toString();
            CustomerValidationRequest request = CustomerValidationRequest.builder()
                    .customerId(customerId)
                    .requestId(requestId)
                    .timestamp(LocalDateTime.now())
                    .build();

            log.info("Storing pending request with ID: {}", requestId);
            requestResponseStore.putRequest(requestId, null);

            log.info("Sending customer validation request via Kafka: requestId={}, customerId={}",
                    requestId, customerId);
            kafkaProducerService.sendCustomerValidationRequest(request);

            log.info("Waiting for customer validation response (timeout: {} seconds)...", requestTimeoutSeconds);
            CustomerValidationResponse response = requestResponseStore
                    .getResponse(requestId, CustomerValidationResponse.class, requestTimeoutSeconds, TimeUnit.SECONDS);

            log.info("Received customer validation response: response={}, valid={}, active={}",
                    response != null ? "present" : "NULL",
                    response != null ? response.getValid() : "N/A",
                    response != null ? response.getActive() : "N/A");

            if (response == null || !Boolean.TRUE.equals(response.getValid())) {
                log.error("Customer validation FAILED: response null or invalid. Throwing CustomerNotFoundException");
                throw new CustomerNotFoundException("Customer not found with ID: " + customerId);
            }

            if (!Boolean.TRUE.equals(response.getActive())) {
                log.error("Customer account is INACTIVE. Throwing InvalidCalculationDataException");
                throw new InvalidCalculationDataException("Customer account is not active");
            }

            log.info("Customer validation SUCCESSFUL for ID: {}", customerId);
            log.info("============ VALIDATION COMPLETE ============");
        } catch (InterruptedException e) {
            log.error("Interrupted while validating customer with ID: {}", customerId, e);
            throw new ServiceIntegrationException("Failed to validate customer information", e);
        }
    }

    private ProductResponse fetchProductDetails(String productCode) {
        try {
            String requestId = UUID.randomUUID().toString();
            log.info("============ REQUESTING PRODUCT DETAILS ============");
            log.info("Product Code: {}", productCode);
            log.info("Request ID: {}", requestId);

            ProductDetailsRequest request = ProductDetailsRequest.builder()
                    .productCode(productCode)
                    .requestId(requestId)
                    .timestamp(LocalDateTime.now())
                    .build();

            requestResponseStore.putRequest(requestId, null);
            log.info("Stored request marker in Redis");

            kafkaProducerService.sendProductDetailsRequest(request);
            log.info("Sent product details request to Kafka");

            log.info("Waiting for response (timeout: {} seconds)...", requestTimeoutSeconds);
            ProductDetailsResponse response = requestResponseStore
                    .getResponse(requestId, ProductDetailsResponse.class, requestTimeoutSeconds, TimeUnit.SECONDS);

            log.info("Response received: {}", response);
            log.info("Response product ID: {}", response != null ? response.getProductId() : "NULL");

            if (response == null || response.getProductId() == null) {
                log.error("Product not found - response null or productId null");
                throw new ProductNotFoundException("Product not found with code: " + productCode);
            }

            if (!"ACTIVE".equals(response.getStatus())) {
                throw new InvalidCalculationDataException("Product is not active: " + productCode);
            }

            return convertToProductResponse(response);
        } catch (InterruptedException e) {
            log.error("Interrupted while fetching product details for code: {}", productCode, e);
            throw new ServiceIntegrationException("Failed to fetch product information", e);
        }
    }

    private String fetchProductNameSafely(String productCode) {
        try {
            ProductResponse product = fetchProductDetails(productCode);
            return product.getProductName();
        } catch (Exception e) {
            log.warn("Failed to fetch product name for code: {}", productCode);
            return productCode;
        }
    }

    private ProductResponse convertToProductResponse(ProductDetailsResponse response) {
        return ProductResponse.builder()
                .id(response.getProductId())
                .productCode(response.getProductCode())
                .productName(response.getProductName())
                .status(response.getStatus())
                .minAmount(response.getMinAmount())
                .maxAmount(response.getMaxAmount())
                .minTermMonths(response.getMinTermMonths())
                .maxTermMonths(response.getMaxTermMonths())
                .minInterestRate(response.getMinInterestRate())
                .maxInterestRate(response.getMaxInterestRate())
                .currency(response.getCurrency())
                .build();
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
        double t = tenureMonths.doubleValue() / 12.0;

        if (compoundingFrequency != null && compoundingFrequency == 0) {
            double maturitySimple = p * (1 + (r * t));
            return BigDecimal.valueOf(maturitySimple).setScale(roundingScale, RoundingMode.HALF_UP);
        }

        double n = compoundingFrequency != null ? compoundingFrequency.doubleValue() : 1.0;
        double maturity = p * Math.pow(1 + (r / n), n * t);
        return BigDecimal.valueOf(maturity).setScale(roundingScale, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateEffectiveRate(BigDecimal nominalRate, Integer compoundingFrequency) {
        double r = nominalRate.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP).doubleValue();
        if (compoundingFrequency != null && compoundingFrequency == 0) {
            double eff = r * 100.0;
            return BigDecimal.valueOf(eff).setScale(roundingScale, RoundingMode.HALF_UP);
        }
        double n = compoundingFrequency != null ? compoundingFrequency.doubleValue() : 1.0;
        double effectiveRate = (Math.pow(1 + (r / n), n) - 1) * 100;
        return BigDecimal.valueOf(effectiveRate).setScale(roundingScale, RoundingMode.HALF_UP);
    }

    private Integer resolveCompoundingFrequency(FdCalculationRequest request, ProductResponse product) {
        if (request.getCompoundingFrequency() != null)
            return request.getCompoundingFrequency();
        if (product != null && product.getCompoundingFrequency() != null) {
            String f = product.getCompoundingFrequency().toUpperCase();
            switch (f) {
                case "DAILY":
                    return 365;
                case "MONTHLY":
                    return 12;
                case "QUARTERLY":
                    return 4;
                case "SEMI_ANNUAL":
                    return 2;
                case "ANNUAL":
                    return 1;
                case "SIMPLE":
                    return 0;
                default:
                    break;
            }
        }
        return defaultCompoundingFrequency;
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
