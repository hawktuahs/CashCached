package com.bt.customer.event;

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
}
