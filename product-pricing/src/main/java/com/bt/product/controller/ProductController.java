package com.bt.product.controller;

import com.bt.product.dto.*;
import com.bt.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/product")
@RequiredArgsConstructor
@Tag(name = "Product Management", description = "APIs for managing banking products")
@SecurityRequirement(name = "Bearer Authentication")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'BANKOFFICER')")
    @Operation(summary = "Create new product", description = "Creates a new banking product with configuration and pricing rules")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@Valid @RequestBody ProductRequest request) {
        ProductResponse response = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<ProductResponse>builder()
                .success(true)
                .message("Product created successfully")
                .data(response)
                .build());
    }

    @PutMapping("/{code}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BANKOFFICER')")
    @Operation(summary = "Update product", description = "Updates an existing product's configuration")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(@PathVariable String code,
            @Valid @RequestBody ProductRequest request) {
        ProductResponse response = productService.updateProduct(code, request);
        return ResponseEntity.ok(ApiResponse.<ProductResponse>builder()
                .success(true)
                .message("Product updated successfully")
                .data(response)
                .build());
    }

    @GetMapping("/{code}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'BANKOFFICER')")
    @Operation(summary = "Get product by code", description = "Retrieves product details by product code")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductByCode(@PathVariable String code) {
        ProductResponse response = productService.getProductByCode(code);
        return ResponseEntity.ok(ApiResponse.<ProductResponse>builder()
                .success(true)
                .message("Product retrieved successfully")
                .data(response)
                .build());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'BANKOFFICER')")
    @Operation(summary = "Get all products", description = "Retrieves all available banking products")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getAllProducts() {
        List<ProductResponse> responses = productService.getAllProducts();
        return ResponseEntity.ok(ApiResponse.<List<ProductResponse>>builder()
                .success(true)
                .message("Products retrieved successfully")
                .data(responses)
                .build());
    }

    @PostMapping("/search")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'BANKOFFICER')")
    @Operation(summary = "Search products", description = "Searches products by type, currency, status, and dates")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> searchProducts(
            @RequestBody ProductSearchRequest searchRequest) {
        List<ProductResponse> responses = productService.searchProducts(searchRequest);
        return ResponseEntity.ok(ApiResponse.<List<ProductResponse>>builder()
                .success(true)
                .message("Search completed successfully")
                .data(responses)
                .build());
    }

    @GetMapping("/status/{code}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'BANKOFFICER')")
    @Operation(summary = "Get product status", description = "Retrieves product status, validity, and applicable pricing rules")
    public ResponseEntity<ApiResponse<ProductStatusResponse>> getProductStatus(@PathVariable String code) {
        ProductStatusResponse response = productService.getProductStatus(code);
        return ResponseEntity.ok(ApiResponse.<ProductStatusResponse>builder()
                .success(true)
                .message("Product status retrieved successfully")
                .data(response)
                .build());
    }

    @DeleteMapping("/{code}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete product", description = "Deletes a product by product code (Admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable String code) {
        productService.deleteProduct(code);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Product deleted successfully")
                .build());
    }
}
