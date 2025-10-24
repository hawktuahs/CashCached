package com.bt.accounts.client;

import com.bt.accounts.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "customer-service", url = "${services.customer.url}")
public interface CustomerServiceClient {

    @GetMapping("/api/v1/customers/{customerId}")
    ApiResponse<CustomerDto> getCustomerById(
            @PathVariable("customerId") String customerId,
            @RequestHeader("Authorization") String token);

    @GetMapping("/api/v1/customers/validate")
    ApiResponse<Boolean> validateCustomer(
            @RequestHeader("Authorization") String token);

    @GetMapping("/api/customer/profile")
    ApiResponse<CustomerDto> getCurrentProfile(
            @RequestHeader("Authorization") String token);
}
