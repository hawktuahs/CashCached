package com.bt.fixeddeposit.client;

import com.bt.fixeddeposit.dto.external.CustomerResponse;
import com.bt.fixeddeposit.dto.external.ExternalApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "customer-service", url = "${services.customer.url}")
public interface CustomerServiceClient {

    @GetMapping("/api/v1/customers/{id}")
    ExternalApiResponse<CustomerResponse> getCustomerById(
            @PathVariable("id") Long id,
            @RequestHeader("Authorization") String token);

    @GetMapping("/api/v1/customers/validate")
    ExternalApiResponse<Boolean> validateCustomer(
            @RequestHeader("Authorization") String token);
}
