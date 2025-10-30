package com.bt.fixeddeposit.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerService {

    private final RequestResponseStore requestResponseStore;

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
                log.info("Storing response in RequestResponseStore for requestId: {}", response.getRequestId());
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
            log.info("Received product details response for request: {}",
                    response != null ? response.getRequestId() : "null response");
            if (response != null && response.getRequestId() != null) {
                requestResponseStore.putResponse(response.getRequestId(), response);
            } else {
                log.error("Invalid product details response: {}", response);
            }
        } catch (Exception e) {
            log.error("Error processing product details response", e);
        }
    }
}
