package com.bt.accounts.controller;

import com.bt.accounts.dto.*;
import com.bt.accounts.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2/accounts")
@RequiredArgsConstructor
@Tag(name = "Account Management V2", description = "V2 APIs for creating FD accounts with customizable values (interest rate, tenure) within product limits")
@SecurityRequirement(name = "Bearer Authentication")
public class AccountControllerV2 {

    private final AccountService accountService;

    @PostMapping
    @PreAuthorize("hasAnyRole('BANKOFFICER', 'ADMIN', 'CUSTOMER')")
    @Operation(summary = "Create FD account with custom values (V2)", description = "Creates a Fixed Deposit account with optional custom interest rate and tenure. "
            +
            "Custom values must be within the product's allowed range. " +
            "Generates both standard account number and IBAN-compliant number. " +
            "If custom values are not provided, product defaults will be used.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Account created successfully with custom or default values", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AccountResponse.class), examples = @ExampleObject(value = "{\"success\": true, \"message\": \"Account created successfully\", \"data\": {\"accountNo\": \"IN97SBIN0BR001000000000000000000011\", \"customerId\": \"12345\", \"productCode\": \"FD_PREMIUM\", \"interestRate\": 7.5, \"tenureMonths\": 24}}"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request data or custom values outside product range", content = @Content(mediaType = "application/json")),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Customer or Product not found", content = @Content(mediaType = "application/json")),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content(mediaType = "application/json"))
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Account creation request with optional custom interest rate and tenure. Must be within product limits.", required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = AccountCreationV2Request.class), examples = @ExampleObject(value = "{\"customerId\": \"12345\", \"productCode\": \"FD_PREMIUM\", \"principalAmount\": 50000, \"customInterestRate\": 7.5, \"customTenureMonths\": 24, \"branchCode\": \"BR001\", \"remarks\": \"Premium FD with custom rate\"}")))
    public ResponseEntity<com.bt.accounts.dto.ApiResponse<AccountResponse>> createAccount(
            @Valid @RequestBody AccountCreationV2Request request,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authToken) {

        AccountResponse account = accountService.createAccountV2(request, authToken);

        com.bt.accounts.dto.ApiResponse<AccountResponse> response = com.bt.accounts.dto.ApiResponse
                .<AccountResponse>builder()
                .success(true)
                .message("Account created successfully with custom values")
                .data(account)
                .build();

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}
