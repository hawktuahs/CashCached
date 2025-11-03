package com.bt.product.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

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
}
