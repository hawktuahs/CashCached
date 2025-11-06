package com.bt.accounts.controller;

import com.bt.accounts.dto.*;
import com.bt.accounts.service.RedemptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts/redemption")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Account Redemption", description = "APIs for FD account redemption enquiry and processing with blockchain integration")
@SecurityRequirement(name = "Bearer Authentication")
public class RedemptionController {

    private final RedemptionService redemptionService;

    @GetMapping("/{accountNo}/enquiry")
    @PreAuthorize("hasAnyRole('BANKOFFICER', 'ADMIN', 'CUSTOMER')")
    @Operation(summary = "Get redemption enquiry details", description = "Retrieves detailed redemption information including accrued interest, penalties, net payout amount, "
            +
            "and blockchain wallet balance. Supports both maturity and premature redemption scenarios.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Redemption enquiry retrieved successfully", content = @Content(schema = @Schema(implementation = RedemptionEnquiryResponse.class))),
            @ApiResponse(responseCode = "404", description = "Account not found", content = @Content(schema = @Schema(implementation = com.bt.accounts.dto.ApiResponse.class))),
            @ApiResponse(responseCode = "400", description = "Account is not active or invalid request", content = @Content(schema = @Schema(implementation = com.bt.accounts.dto.ApiResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing authentication token", content = @Content)
    })
    public ResponseEntity<com.bt.accounts.dto.ApiResponse<RedemptionEnquiryResponse>> getRedemptionEnquiry(
            @Parameter(description = "FD Account number", required = true, example = "FD001-2024-00001") @PathVariable String accountNo,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        log.info("Processing redemption enquiry for account: {}", accountNo);
        String bearer = authHeader != null ? authHeader : "";
        RedemptionEnquiryResponse enquiry = redemptionService.getRedemptionEnquiry(accountNo, bearer);

        com.bt.accounts.dto.ApiResponse<RedemptionEnquiryResponse> response = com.bt.accounts.dto.ApiResponse
                .<RedemptionEnquiryResponse>builder()
                .success(true)
                .message("Redemption enquiry retrieved successfully")
                .data(enquiry)
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{accountNo}/process")
    @PreAuthorize("hasAnyRole('BANKOFFICER', 'ADMIN', 'CUSTOMER')")
    @Operation(summary = "Process account redemption", description = "Processes FD account redemption with blockchain token transfer. "
            +
            "Supports both maturity payout and premature closure with penalty. " +
            "Automatically transfers tokens to customer's blockchain wallet and records transaction. " +
            "Publishes redemption event to Kafka for downstream processing.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Redemption processed successfully", content = @Content(schema = @Schema(implementation = RedemptionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Account not found", content = @Content(schema = @Schema(implementation = com.bt.accounts.dto.ApiResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid redemption request - Account not active, penalty not accepted, or invalid parameters", content = @Content(schema = @Schema(implementation = com.bt.accounts.dto.ApiResponse.class))),
            @ApiResponse(responseCode = "500", description = "Blockchain integration failed or service error", content = @Content(schema = @Schema(implementation = com.bt.accounts.dto.ApiResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing authentication token", content = @Content)
    })
    public ResponseEntity<com.bt.accounts.dto.ApiResponse<RedemptionResponse>> processRedemption(
            @Parameter(description = "FD Account number", required = true, example = "FD001-2024-00001") @PathVariable String accountNo,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        log.info("Processing redemption for account: {}", accountNo);
        String bearer = authHeader != null ? authHeader : "";

        RedemptionResponse redemption = redemptionService.processRedemption(accountNo, null, bearer);

        com.bt.accounts.dto.ApiResponse<RedemptionResponse> response = com.bt.accounts.dto.ApiResponse
                .<RedemptionResponse>builder()
                .success(true)
                .message("Account redeemed successfully")
                .data(redemption)
                .build();

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/{accountNo}/eligibility")
    @PreAuthorize("hasAnyRole('BANKOFFICER', 'ADMIN', 'CUSTOMER')")
    @Operation(summary = "Check redemption eligibility", description = "Quick check to determine if account is eligible for redemption. "
            +
            "Returns eligibility status, warnings, and basic payout information.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Eligibility check completed", content = @Content(schema = @Schema(implementation = RedemptionEnquiryResponse.class))),
            @ApiResponse(responseCode = "404", description = "Account not found", content = @Content(schema = @Schema(implementation = com.bt.accounts.dto.ApiResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
    })
    public ResponseEntity<com.bt.accounts.dto.ApiResponse<RedemptionEnquiryResponse>> checkRedemptionEligibility(
            @Parameter(description = "FD Account number", required = true, example = "FD001-2024-00001") @PathVariable String accountNo,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        log.info("Checking redemption eligibility for account: {}", accountNo);

        String bearer = authHeader != null ? authHeader : "";
        RedemptionEnquiryResponse enquiry = redemptionService.getRedemptionEnquiry(accountNo, bearer);

        com.bt.accounts.dto.ApiResponse<RedemptionEnquiryResponse> response = com.bt.accounts.dto.ApiResponse
                .<RedemptionEnquiryResponse>builder()
                .success(true)
                .message("Eligibility check completed")
                .data(enquiry)
                .build();

        return ResponseEntity.ok(response);
    }
}
