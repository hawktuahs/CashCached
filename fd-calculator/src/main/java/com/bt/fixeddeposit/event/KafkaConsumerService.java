package com.bt.fixeddeposit.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerService {

    private final KafkaProducerService kafkaProducerService;

    @KafkaListener(topics = "customer.validation.request", groupId = "fd-calculator-customer-validator")
    public void handleCustomerValidationRequest(CustomerValidationRequest request) {
        log.info("Received customer validation request for customer ID: {}", request.getCustomerId());

        CustomerValidationResponse response = CustomerValidationResponse.builder()
                .requestId(request.getRequestId())
                .customerId(request.getCustomerId())
                .timestamp(LocalDateTime.now())
                .valid(true)
                .active(true)
                .build();

        kafkaProducerService.sendCustomerValidationResponse(response);
    }

    @KafkaListener(topics = "product.details.request", groupId = "fd-calculator-product-details")
    public void handleProductDetailsRequest(ProductDetailsRequest request) {
        log.info("Received product details request for product code: {}", request.getProductCode());

        ProductDetailsResponse response = ProductDetailsResponse.builder()
                .requestId(request.getRequestId())
                .productCode(request.getProductCode())
                .timestamp(LocalDateTime.now())
                .status("ACTIVE")
                .build();

        kafkaProducerService.sendProductDetailsResponse(response);
    }
}
