package com.bt.product.service.kafka;

import com.bt.product.event.KafkaProducerService;
import com.bt.product.event.ProductDetailsRequest;
import com.bt.product.event.ProductDetailsResponse;
import com.bt.product.service.ProductService;
import com.bt.product.dto.ProductResponse;
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

    private final ProductService productService;
    private final KafkaProducerService kafkaProducerService;

    @KafkaListener(topics = "product.details.request", groupId = "product-details-consumer")
    public void handleProductDetailsRequest(@Payload ProductDetailsRequest request) {
        try {
            log.info("Received product details request for product code: {}",
                    request != null ? request.getProductCode() : "null request");

            if (request == null || request.getProductCode() == null) {
                log.error("Invalid product details request");
                return;
            }

            ProductResponse product = productService.getProductByCode(request.getProductCode());

            ProductDetailsResponse response = ProductDetailsResponse.builder()
                    .requestId(request.getRequestId())
                    .productId(product.getId())
                    .productCode(product.getProductCode())
                    .productName(product.getProductName())
                    .status(product.getStatus().toString())
                    .minAmount(product.getMinAmount())
                    .maxAmount(product.getMaxAmount())
                    .minTermMonths(product.getMinTermMonths())
                    .maxTermMonths(product.getMaxTermMonths())
                    .minInterestRate(product.getMinInterestRate())
                    .maxInterestRate(product.getMaxInterestRate())
                    .currency(product.getCurrency() != null ? product.getCurrency().toString() : "USD")
                    .compoundingFrequency(product.getCompoundingFrequency())
                    .timestamp(LocalDateTime.now())
                    .build();

            kafkaProducerService.sendProductDetailsResponse(response);
            log.info("Product details response sent for request: {}", request.getRequestId());
        } catch (Exception e) {
            log.error("Error handling product details request for code: {}",
                    request != null ? request.getProductCode() : "unknown", e);

            ProductDetailsResponse errorResponse = ProductDetailsResponse.builder()
                    .requestId(request != null ? request.getRequestId() : "unknown")
                    .productCode(request != null ? request.getProductCode() : null)
                    .error(e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();

            kafkaProducerService.sendProductDetailsResponse(errorResponse);
        }
    }
}
