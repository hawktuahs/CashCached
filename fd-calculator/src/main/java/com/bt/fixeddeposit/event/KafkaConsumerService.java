package com.bt.fixeddeposit.event;

import com.bt.fixeddeposit.dto.FdCalculationRequest;
import com.bt.fixeddeposit.service.FdCalculationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerService {

    private final RedisRequestResponseStore requestResponseStore;
    private final FdCalculationService fdCalculationService;
    private final KafkaProducerService kafkaProducerService;

    @KafkaListener(topics = "customer.validation.response", groupId = "fd-customer-response-consumer", containerFactory = "customerValidationKafkaListenerContainerFactory")
    public void handleCustomerValidationResponse(@Payload CustomerValidationResponse response) {
        try {
            log.info("============ KAFKA RESPONSE RECEIVED ============");
            log.info("Customer validation response: requestId={}, customerId={}, valid={}, active={}",
                    response != null ? response.getRequestId() : "null",
                    response != null ? response.getCustomerId() : "N/A",
                    response != null ? response.getValid() : "N/A",
                    response != null ? response.getActive() : "N/A");

            if (response != null && response.getRequestId() != null) {
                log.info("Storing response in RedisRequestResponseStore for requestId: {}", response.getRequestId());
                requestResponseStore.putResponse(response.getRequestId(), response);
                log.info("Response stored successfully");
            } else {
                log.error("Invalid customer validation response - null response or null requestId");
            }
            log.info("============ KAFKA RESPONSE PROCESSED ============");
        } catch (Exception e) {
            log.error("Error processing customer validation response", e);
        }
    }

    @KafkaListener(topics = "product.details.response", groupId = "fd-product-response-consumer", containerFactory = "productDetailsKafkaListenerContainerFactory")
    public void handleProductDetailsResponse(@Payload ProductDetailsResponse response) {
        try {
            log.info("============ PRODUCT DETAILS RESPONSE RECEIVED ============");
            log.info("Response: {}", response);
            log.info("Request ID: {}", response != null ? response.getRequestId() : "NULL");
            log.info("Product ID: {}", response != null ? response.getProductId() : "NULL");
            log.info("Product Code: {}", response != null ? response.getProductCode() : "NULL");
            log.info("Error: {}", response != null ? response.getError() : "NULL");

            if (response != null && response.getRequestId() != null) {
                log.info("Storing response in Redis for request ID: {}", response.getRequestId());
                requestResponseStore.putResponse(response.getRequestId(), response);
                log.info("Response stored successfully");
            } else {
                log.error("Invalid product details response - null response or null requestId");
            }
        } catch (Exception e) {
            log.error("Error processing product details response", e);
        }
    }

    @KafkaListener(topics = "fd.calculation.request", groupId = "fd-calculation-consumer", containerFactory = "fdCalculationRequestKafkaListenerContainerFactory")
    public void handleFdCalculationRequest(@Payload FdCalculationRequestEvent request) {
        try {
            log.info("============ FD CALCULATION REQUEST RECEIVED ============");
            log.info("Request ID: {}, Customer ID: {}, Product: {}, Principal: {}, Tenure: {} months",
                    request.getRequestId(), request.getCustomerId(), request.getProductCode(),
                    request.getPrincipalAmount(), request.getTenureMonths());

            if (request == null || request.getRequestId() == null) {
                log.error("Invalid FD calculation request - null request or null requestId");
                return;
            }

            // Convert event to service request
            FdCalculationRequest calcRequest = new FdCalculationRequest();
            calcRequest.setCustomerId(request.getCustomerId());
            calcRequest.setProductCode(request.getProductCode());
            calcRequest.setPrincipalAmount(request.getPrincipalAmount());
            calcRequest.setTenureMonths(request.getTenureMonths());

            // Perform calculation
            var calcResponse = fdCalculationService.calculateFd(calcRequest, null);

            // Build response event
            FdCalculationResponseEvent response = FdCalculationResponseEvent.builder()
                    .requestId(request.getRequestId())
                    .calculationId(calcResponse.getId())
                    .customerId(request.getCustomerId())
                    .productCode(request.getProductCode())
                    .principalAmount(calcResponse.getPrincipalAmount())
                    .maturityAmount(calcResponse.getMaturityAmount())
                    .interestEarned(calcResponse.getInterestEarned())
                    .effectiveRate(calcResponse.getEffectiveRate())
                    .tenureMonths(calcResponse.getTenureMonths())
                    .timestamp(LocalDateTime.now())
                    .build();

            kafkaProducerService.sendFdCalculationResponse(response);
            log.info("FD calculation response sent successfully for request: {}", request.getRequestId());
            log.info("============ FD CALCULATION REQUEST PROCESSED ============");
        } catch (Exception e) {
            log.error("Error processing FD calculation request for requestId: {}",
                    request != null ? request.getRequestId() : "unknown", e);

            // Send error response
            FdCalculationResponseEvent errorResponse = FdCalculationResponseEvent.builder()
                    .requestId(request != null ? request.getRequestId() : "unknown")
                    .customerId(request != null ? request.getCustomerId() : null)
                    .productCode(request != null ? request.getProductCode() : null)
                    .error(e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();

            kafkaProducerService.sendFdCalculationResponse(errorResponse);
        }
    }
}
