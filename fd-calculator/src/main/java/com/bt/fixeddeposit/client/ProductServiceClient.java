package com.bt.fixeddeposit.client;

import com.bt.fixeddeposit.dto.external.ExternalApiResponse;
import com.bt.fixeddeposit.dto.external.ProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "product-service", url = "${services.product.url}")
public interface ProductServiceClient {

    @GetMapping("/api/v1/product/{productCode}")
    ExternalApiResponse<ProductResponse> getProductByCode(
            @PathVariable("productCode") String productCode,
            @RequestHeader("Authorization") String token);

    @GetMapping("/api/v1/product/{id}")
    ExternalApiResponse<ProductResponse> getProductById(
            @PathVariable("id") Long id,
            @RequestHeader("Authorization") String token);
}
