package com.bt.customer.service.kafka;

import com.bt.customer.event.KafkaProducerService;
import com.bt.customer.event.CustomerValidationRequest;
import com.bt.customer.event.CustomerValidationResponse;
import com.bt.customer.repository.UserRepository;
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

    private final UserRepository userRepository;
    private final KafkaProducerService kafkaProducerService;

    @KafkaListener(topics = "customer.validation.request", groupId = "customer-validation-consumer")
    public void handleCustomerValidationRequest(@Payload CustomerValidationRequest request) {
        try {
            log.info("============ KAFKA CONSUMER START ============");
            log.info("Received customer validation request: requestId={}, customerId={}",
                    request != null ? request.getRequestId() : "null",
                    request != null ? request.getCustomerId() : "null request");

            if (request == null || request.getCustomerId() == null) {
                log.error("Invalid customer validation request - null request or null customerId");
                return;
            }

            log.info("Checking if customer exists with ID: {}", request.getCustomerId());
            boolean customerExists = userRepository.existsById(request.getCustomerId());
            log.info("Customer exists check result: {}", customerExists);

            boolean isActive = false;

            if (customerExists) {
                log.info("Fetching customer details for ID: {}", request.getCustomerId());
                var user = userRepository.findById(request.getCustomerId());
                log.info("User found: {}, Active status: {}",
                        user.isPresent(),
                        user.isPresent() ? user.get().getActive() : "N/A");
                isActive = user.isPresent() && Boolean.TRUE.equals(user.get().getActive());
            } else {
                log.warn("Customer with ID {} does NOT exist in database", request.getCustomerId());
            }

            CustomerValidationResponse response = CustomerValidationResponse.builder()
                    .requestId(request.getRequestId())
                    .customerId(request.getCustomerId())
                    .timestamp(LocalDateTime.now())
                    .valid(customerExists)
                    .active(isActive)
                    .build();

            log.info("Sending validation response: requestId={}, valid={}, active={}",
                    response.getRequestId(), response.getValid(), response.getActive());
            kafkaProducerService.sendCustomerValidationResponse(response);
            log.info("Customer validation response sent successfully for request: {}", request.getRequestId());
            log.info("============ KAFKA CONSUMER END ============");
        } catch (Exception e) {
            log.error("Error validating customer with ID: {}",
                    request != null ? request.getCustomerId() : "unknown", e);

            CustomerValidationResponse errorResponse = CustomerValidationResponse.builder()
                    .requestId(request != null ? request.getRequestId() : "unknown")
                    .customerId(request != null ? request.getCustomerId() : null)
                    .timestamp(LocalDateTime.now())
                    .valid(false)
                    .active(false)
                    .error(e.getMessage())
                    .build();

            kafkaProducerService.sendCustomerValidationResponse(errorResponse);
        }
    }
}
