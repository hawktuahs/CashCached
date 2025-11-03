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
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Tag(name = "Account Management V1", description = "V1 APIs for creating FD accounts with product default values (interest rate, tenure)")
@SecurityRequirement(name = "Bearer Authentication")
public class AccountControllerV1 {

    private final AccountService accountService;

    @PostMapping
    @PreAuthorize("hasAnyRole('BANKOFFICER', 'ADMIN', 'CUSTOMER')")
    @Operation(summary = "Create FD account with product defaults (V1)", description = "Creates a Fixed Deposit account using default interest rate and tenure from the product. "
            +
            "Generates both standard account number and IBAN-compliant number. " +
            "Values are defaulted from product configuration.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Account created successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AccountResponse.class), examples = @ExampleObject(value = "{\"success\": true, \"message\": \"Account created successfully\", \"data\": {\"accountNo\": \"IN97SBIN0BR001000000000000000000011\", \"customerId\": \"12345\", \"productCode\": \"FD_STANDARD\"}}"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content(mediaType = "application/json")),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Customer or Product not found", content = @Content(mediaType = "application/json")),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content(mediaType = "application/json"))
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Account creation request with minimal required fields. Interest rate and tenure will be set from product defaults.", required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = AccountCreationV1Request.class), examples = @ExampleObject(value = "{\"customerId\": \"12345\", \"productCode\": \"FD_STANDARD\", \"principalAmount\": 10000, \"branchCode\": \"BR001\", \"remarks\": \"Standard FD account\"}")))
    public ResponseEntity<com.bt.accounts.dto.ApiResponse<AccountResponse>> createAccount(
            @Valid @RequestBody AccountCreationV1Request request,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authToken) {

        AccountResponse account = accountService.createAccountV1(request, authToken);

        com.bt.accounts.dto.ApiResponse<AccountResponse> response = com.bt.accounts.dto.ApiResponse
                .<AccountResponse>builder()
                .success(true)
                .message("Account created successfully with product defaults")
                .data(account)
                .build();

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}
