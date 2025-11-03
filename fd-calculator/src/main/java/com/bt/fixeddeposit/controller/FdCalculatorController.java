package com.bt.fixeddeposit.controller;

import com.bt.fixeddeposit.dto.ApiResponse;
import com.bt.fixeddeposit.dto.FdCalculationRequest;
import com.bt.fixeddeposit.dto.FdCalculationResponse;
import com.bt.fixeddeposit.service.FdCalculationService;
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
@RequestMapping("/api/fd")
@RequiredArgsConstructor
@Tag(name = "FD Calculator", description = "Fixed Deposit calculation and management APIs")
@SecurityRequirement(name = "Bearer Authentication")
public class FdCalculatorController {

    private final FdCalculationService calculationService;

    @PostMapping("/calculate")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BANKOFFICER', 'ADMIN')")
    @Operation(summary = "Calculate FD maturity", description = "Calculate Fixed Deposit maturity amount based on principal, tenure, and product")
    public ResponseEntity<ApiResponse<FdCalculationResponse>> calculateFd(
            @Valid @RequestBody FdCalculationRequest request,
            @RequestHeader("Authorization") String authToken) {

        FdCalculationResponse response = calculationService.calculateFd(request, authToken);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "FD calculation completed successfully"));
    }

    @GetMapping("/calculations/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BANKOFFICER', 'ADMIN')")
    @Operation(summary = "Get calculation by ID", description = "Retrieve a specific FD calculation by its ID")
    public ResponseEntity<ApiResponse<FdCalculationResponse>> getCalculationById(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authToken) {

        FdCalculationResponse response = calculationService.getCalculationById(id, authToken);
        return ResponseEntity.ok(ApiResponse.success(response, "Calculation retrieved successfully"));
    }

    @GetMapping("/history/{customerId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BANKOFFICER', 'ADMIN')")
    @Operation(summary = "Get customer calculation history", description = "Retrieve all FD calculations for a specific customer")
    public ResponseEntity<ApiResponse<List<FdCalculationResponse>>> getCalculationHistory(
            @PathVariable Long customerId,
            @RequestHeader("Authorization") String authToken) {

        List<FdCalculationResponse> response = calculationService.getCalculationHistory(customerId, authToken);
        return ResponseEntity.ok(ApiResponse.success(response, "Calculation history retrieved successfully"));
    }

    @GetMapping("/recent/{customerId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BANKOFFICER', 'ADMIN')")
    @Operation(summary = "Get recent calculations", description = "Retrieve recent FD calculations for a customer within specified days")
    public ResponseEntity<ApiResponse<List<FdCalculationResponse>>> getRecentCalculations(
            @PathVariable Long customerId,
            @RequestParam(defaultValue = "30") Integer days,
            @RequestHeader("Authorization") String authToken) {

        List<FdCalculationResponse> response = calculationService.getRecentCalculations(customerId, days, authToken);
        return ResponseEntity.ok(ApiResponse.success(response, "Recent calculations retrieved successfully"));
    }
}
