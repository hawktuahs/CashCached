package com.bt.accounts.client;

import com.bt.accounts.dto.ApiResponse;
import com.bt.accounts.dto.PricingRuleDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@FeignClient(name = "product-service", url = "${services.product.url}")
public interface ProductServiceClient {

    @GetMapping("/api/v1/product/{id}")
    ApiResponse<ProductDto> getProductById(
            @PathVariable("id") Long id,
            @RequestHeader("Authorization") String token);

    @GetMapping("/api/v1/product/{productCode}")
    ApiResponse<ProductDto> getProductByCode(
            @PathVariable("productCode") String productCode,
            @RequestHeader("Authorization") String token);

    @GetMapping("/api/v1/pricing-rule/product/{productId}/active")
    ApiResponse<List<PricingRuleDto>> getActivePricingRules(
            @PathVariable("productId") Long productId,
            @RequestHeader("Authorization") String token);
}
