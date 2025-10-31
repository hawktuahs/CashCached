package com.bt.product.controller;

import com.bt.product.dto.ApiResponse;
import com.bt.product.dto.PricingRuleRequest;
import com.bt.product.dto.PricingRuleResponse;
import com.bt.product.service.PricingRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/pricing-rule")
@RequiredArgsConstructor
@Tag(name = "Pricing Rule Management", description = "APIs for managing product pricing rules")
@SecurityRequirement(name = "Bearer Authentication")
public class PricingRuleController {

    private final PricingRuleService pricingRuleService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'BANKOFFICER')")
    @Operation(summary = "Create pricing rule", description = "Creates a new pricing rule for a product")
    public ResponseEntity<ApiResponse<PricingRuleResponse>> createPricingRule(
            @Valid @RequestBody PricingRuleRequest request) {
        PricingRuleResponse response = pricingRuleService.createPricingRule(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<PricingRuleResponse>builder()
                .success(true)
                .message("Pricing rule created successfully")
                .data(response)
                .build());
    }

    @PutMapping("/{ruleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BANKOFFICER')")
    @Operation(summary = "Update pricing rule", description = "Updates an existing pricing rule")
    public ResponseEntity<ApiResponse<PricingRuleResponse>> updatePricingRule(@PathVariable Long ruleId,
            @Valid @RequestBody PricingRuleRequest request) {
        PricingRuleResponse response = pricingRuleService.updatePricingRule(ruleId, request);
        return ResponseEntity.ok(ApiResponse.<PricingRuleResponse>builder()
                .success(true)
                .message("Pricing rule updated successfully")
                .data(response)
                .build());
    }

    @GetMapping("/{ruleId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'BANKOFFICER')")
    @Operation(summary = "Get pricing rule by ID", description = "Retrieves a pricing rule by its ID")
    public ResponseEntity<ApiResponse<PricingRuleResponse>> getPricingRuleById(@PathVariable Long ruleId) {
        PricingRuleResponse response = pricingRuleService.getPricingRuleById(ruleId);
        return ResponseEntity.ok(ApiResponse.<PricingRuleResponse>builder()
                .success(true)
                .message("Pricing rule retrieved successfully")
                .data(response)
                .build());
    }

    @GetMapping("/product/{productId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'BANKOFFICER')")
    @Operation(summary = "Get pricing rules by product", description = "Retrieves all pricing rules for a product")
    public ResponseEntity<ApiResponse<List<PricingRuleResponse>>> getPricingRulesByProduct(
            @PathVariable Long productId) {
        List<PricingRuleResponse> responses = pricingRuleService.getPricingRulesByProductId(productId);
        return ResponseEntity.ok(ApiResponse.<List<PricingRuleResponse>>builder()
                .success(true)
                .message("Pricing rules retrieved successfully")
                .data(responses)
                .build());
    }

    @GetMapping("/product/{productId}/active")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'BANKOFFICER')")
    @Operation(summary = "Get active pricing rules", description = "Retrieves active pricing rules for a product ordered by priority")
    public ResponseEntity<ApiResponse<List<PricingRuleResponse>>> getActivePricingRules(@PathVariable Long productId) {
        List<PricingRuleResponse> responses = pricingRuleService.getActivePricingRulesByProductId(productId);
        return ResponseEntity.ok(ApiResponse.<List<PricingRuleResponse>>builder()
                .success(true)
                .message("Active pricing rules retrieved successfully")
                .data(responses)
                .build());
    }

    @GetMapping("/product/{productId}/applicable")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'BANKOFFICER')")
    @Operation(summary = "Get applicable pricing rules", description = "Retrieves pricing rules applicable for a specific amount")
    public ResponseEntity<ApiResponse<List<PricingRuleResponse>>> getApplicableRules(@PathVariable Long productId,
            @RequestParam BigDecimal amount) {
        List<PricingRuleResponse> responses = pricingRuleService.getApplicableRulesForAmount(productId, amount);
        return ResponseEntity.ok(ApiResponse.<List<PricingRuleResponse>>builder()
                .success(true)
                .message("Applicable pricing rules retrieved successfully")
                .data(responses)
                .build());
    }

    @DeleteMapping("/{ruleId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete pricing rule", description = "Deletes a pricing rule by ID (Admin only)")
    public ResponseEntity<ApiResponse<Void>> deletePricingRule(@PathVariable Long ruleId) {
        pricingRuleService.deletePricingRule(ruleId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Pricing rule deleted successfully")
                .build());
    }
}
