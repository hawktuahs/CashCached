package com.bt.accounts.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerService {

    private final RequestResponseStore requestResponseStore;

    @KafkaListener(topics = "customer.validation.response", groupId = "accounts-customer-validator")
    public void handleCustomerValidationResponse(CustomerValidationResponse response) {
        log.info("Received customer validation response for request: {}", response.getRequestId());
        requestResponseStore.putResponse(response.getRequestId(), response);
    }

    @KafkaListener(topics = "product.details.response", groupId = "accounts-product-details")
    public void handleProductDetailsResponse(ProductDetailsResponse response) {
        log.info("Received product details response for request: {}", response.getRequestId());
        requestResponseStore.putResponse(response.getRequestId(), response);
    }

    @KafkaListener(topics = "fd.calculation.response", groupId = "accounts-fd-calculation")
    public void handleFdCalculationResponse(FdCalculationResponseEvent response) {
        log.info("Received FD calculation response for request: {}", response.getRequestId());
        requestResponseStore.putResponse(response.getRequestId(), response);
    }
}
