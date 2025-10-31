package com.bt.customer.controller;

import com.bt.customer.dto.ApiResponse;
import com.bt.customer.dto.UserProfileResponse;
import com.bt.customer.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customers")
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Customer API (External)", description = "External API endpoints for inter-service communication")
public class CustomerApiController {

    @Autowired
    private CustomerService customerService;

    @GetMapping("/{id}")
    @Operation(summary = "Get customer by ID", description = "Retrieves customer details by ID for inter-service communication")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCustomerById(@PathVariable Long id) {
        UserProfileResponse customer = customerService.getCustomerById(id);
        return ResponseEntity.ok(ApiResponse.<UserProfileResponse>builder()
                .success(true)
                .message("Customer retrieved successfully")
                .data(customer)
                .build());
    }

    @GetMapping("/validate")
    @Operation(summary = "Validate customer", description = "Validates if the authenticated customer exists")
    public ResponseEntity<ApiResponse<Boolean>> validateCustomer() {
        try {
            customerService.getCurrentUserProfile();
            return ResponseEntity.ok(ApiResponse.<Boolean>builder()
                    .success(true)
                    .message("Customer is valid")
                    .data(true)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.<Boolean>builder()
                    .success(false)
                    .message("Customer validation failed")
                    .data(false)
                    .build());
        }
    }
}
