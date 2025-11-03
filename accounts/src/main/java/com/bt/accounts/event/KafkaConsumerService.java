package com.bt.accounts.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerService {

    private final RedisRequestResponseStore requestResponseStore;

    @KafkaListener(topics = "customer.validation.response", groupId = "accounts-customer-validator", containerFactory = "customerValidationKafkaListenerContainerFactory")
    public void handleCustomerValidationResponse(CustomerValidationResponse response) {
        log.info("========== ACCOUNTS: Received customer validation response ==========");
        log.info("RequestId: {}, CustomerId: {}, Valid: {}, Active: {}",
                response != null ? response.getRequestId() : "null",
                response != null ? response.getCustomerId() : "null",
                response != null ? response.getValid() : "null",
                response != null ? response.getActive() : "null");

        if (response != null && response.getRequestId() != null) {
            requestResponseStore.putResponse(response.getRequestId(), response);
            log.info("Response stored in Redis for requestId: {}", response.getRequestId());
        } else {
            log.error("Invalid response - null or missing requestId!");
        }
    }

    @KafkaListener(topics = "product.details.response", groupId = "accounts-product-details", containerFactory = "productDetailsKafkaListenerContainerFactory")
    public void handleProductDetailsResponse(ProductDetailsResponse response) {
        log.info("========== ACCOUNTS: Received product details response ==========");
        log.info("RequestId: {}", response != null ? response.getRequestId() : "null");

        if (response != null && response.getRequestId() != null) {
            requestResponseStore.putResponse(response.getRequestId(), response);
            log.info("Response stored for requestId: {}", response.getRequestId());
        }
    }

    @KafkaListener(topics = "fd.calculation.response", groupId = "accounts-fd-calculation", containerFactory = "fdCalculationKafkaListenerContainerFactory")
    public void handleFdCalculationResponse(FdCalculationResponseEvent response) {
        log.info("========== ACCOUNTS: Received FD calculation response ==========");
        log.info("RequestId: {}", response != null ? response.getRequestId() : "null");

        if (response != null && response.getRequestId() != null) {
            requestResponseStore.putResponse(response.getRequestId(), response);
            log.info("Response stored for requestId: {}", response.getRequestId());
        }
    }
}
