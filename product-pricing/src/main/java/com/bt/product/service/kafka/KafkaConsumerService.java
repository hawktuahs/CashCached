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
            log.info("============ PRODUCT DETAILS REQUEST RECEIVED ============");
            log.info("Request: {}", request);
            log.info("Request ID: {}", request != null ? request.getRequestId() : "NULL");
            log.info("Product Code: {}", request != null ? request.getProductCode() : "NULL");

            if (request == null || request.getProductCode() == null) {
                log.error("Invalid product details request - null request or null product code");
                return;
            }

            log.info("Fetching product from service...");
            ProductResponse product = productService.getProductByCode(request.getProductCode());
            log.info("Product fetched: {}", product);

            ProductDetailsResponse response = ProductDetailsResponse.builder()
                    .requestId(request.getRequestId())
                    .productId(product.getId())
                    .productCode(product.getProductCode())
                    .productName(product.getProductName())
                    .productType(product.getProductType())
                    .status(product.getStatus().toString())
                    .minAmount(product.getMinAmount())
                    .maxAmount(product.getMaxAmount())
                    .minTermMonths(product.getMinTermMonths())
                    .maxTermMonths(product.getMaxTermMonths())
                    .minInterestRate(product.getMinInterestRate())
                    .maxInterestRate(product.getMaxInterestRate())
                    .currency(product.getCurrency() != null ? product.getCurrency().toString() : "USD")
                    .compoundingFrequency(product.getCompoundingFrequency())
                    .prematurePenaltyRate(product.getPrematurePenaltyRate())
                    .prematurePenaltyGraceDays(product.getPrematurePenaltyGraceDays())
                    .timestamp(LocalDateTime.now())
                    .build();

            log.info("Sending product details response: {}", response);
            kafkaProducerService.sendProductDetailsResponse(response);
            log.info("Product details response sent successfully for request ID: {}", request.getRequestId());
        } catch (Exception e) {
            log.error("Error handling product details request for code: {}",
                    request != null ? request.getProductCode() : "unknown", e);

            ProductDetailsResponse errorResponse = ProductDetailsResponse.builder()
                    .requestId(request != null ? request.getRequestId() : "unknown")
                    .productCode(request != null ? request.getProductCode() : null)
                    .error(e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();

            log.info("Sending error response: {}", errorResponse);
            kafkaProducerService.sendProductDetailsResponse(errorResponse);
        }
    }
}
