package com.bt.fixeddeposit.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendCustomerValidationRequest(CustomerValidationRequest request) {
        try {
            kafkaTemplate.send(KafkaTopics.CUSTOMER_VALIDATION_REQUEST, request.getRequestId(), request);
            log.info("Customer validation request sent with ID: {}", request.getRequestId());
        } catch (Exception e) {
            log.error("Failed to send customer validation request with ID: {}", request.getRequestId(), e);
        }
    }

    public void sendProductDetailsRequest(ProductDetailsRequest request) {
        try {
            kafkaTemplate.send(KafkaTopics.PRODUCT_DETAILS_REQUEST, request.getRequestId(), request);
            log.info("Product details request sent with ID: {}", request.getRequestId());
        } catch (Exception e) {
            log.error("Failed to send product details request with ID: {}", request.getRequestId(), e);
        }
    }

    public void sendCustomerValidationResponse(CustomerValidationResponse response) {
        try {
            kafkaTemplate.send(KafkaTopics.CUSTOMER_VALIDATION_RESPONSE, response.getRequestId(), response);
            log.info("Customer validation response sent for request: {}", response.getRequestId());
        } catch (Exception e) {
            log.error("Failed to send customer validation response for request: {}", response.getRequestId(), e);
        }
    }

    public void sendProductDetailsResponse(ProductDetailsResponse response) {
        try {
            kafkaTemplate.send(KafkaTopics.PRODUCT_DETAILS_RESPONSE, response.getRequestId(), response);
            log.info("Product details response sent for request: {}", response.getRequestId());
        } catch (Exception e) {
            log.error("Failed to send product details response for request: {}", response.getRequestId(), e);
        }
    }

    public void sendFdCalculationResponse(FdCalculationResponseEvent response) {
        try {
            kafkaTemplate.send(KafkaTopics.FD_CALCULATION_RESPONSE, response.getRequestId(), response);
            log.info("FD calculation response sent for request: {}", response.getRequestId());
        } catch (Exception e) {
            log.error("Failed to send FD calculation response for request: {}", response.getRequestId(), e);
        }
    }
}
