package com.bt.accounts.controller;

import com.bt.accounts.dto.*;
import com.bt.accounts.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Tag(name = "Account Management", description = "APIs for managing Fixed Deposit accounts")
@SecurityRequirement(name = "Bearer Authentication")
public class AccountController {

        private final AccountService accountService;

        @PostMapping("/create")
        @PreAuthorize("hasAnyRole('BANKOFFICER', 'ADMIN', 'CUSTOMER')")
        @Operation(summary = "Create new FD account", description = "Creates a new Fixed Deposit account with validation against product rules and customer verification")
        public ResponseEntity<ApiResponse<AccountResponse>> createAccount(
                        @Valid @RequestBody AccountCreationRequest request,
                        @Parameter(hidden = true) @RequestHeader("Authorization") String authToken) {

                AccountResponse account = accountService.createAccount(request, authToken);

                ApiResponse<AccountResponse> response = ApiResponse.<AccountResponse>builder()
                                .success(true)
                                .message("Account created successfully")
                                .data(account)
                                .build();

                return new ResponseEntity<>(response, HttpStatus.CREATED);
        }

        @GetMapping("/{accountNo}")
        @Operation(summary = "Get account details", description = "Retrieves account information by account number")
        public ResponseEntity<ApiResponse<AccountResponse>> getAccount(
                        @Parameter(description = "FD Account number") @PathVariable String accountNo) {

                AccountResponse account = accountService.getAccount(accountNo);

                ApiResponse<AccountResponse> response = ApiResponse.<AccountResponse>builder()
                                .success(true)
                                .message("Account retrieved successfully")
                                .data(account)
                                .build();

                return ResponseEntity.ok(response);
        }

        @GetMapping("/customer/{customerId}")
        @Operation(summary = "Get customer accounts", description = "Retrieves all FD accounts for a specific customer")
        public ResponseEntity<ApiResponse<List<AccountResponse>>> getCustomerAccounts(
                        @Parameter(description = "Customer ID") @PathVariable String customerId) {

                List<AccountResponse> accounts = accountService.getCustomerAccounts(customerId);

                ApiResponse<List<AccountResponse>> response = ApiResponse.<List<AccountResponse>>builder()
                                .success(true)
                                .message("Customer accounts retrieved successfully")
                                .data(accounts)
                                .build();

                return ResponseEntity.ok(response);
        }

        @PostMapping("/search")
        @PreAuthorize("hasAnyRole('BANKOFFICER', 'ADMIN')")
        @Operation(summary = "Search accounts with pagination", description = "Search and filter FD accounts with pagination support")
        public ResponseEntity<ApiResponse<PagedResponse<AccountResponse>>> searchAccounts(
                        @Valid @RequestBody AccountSearchRequest request) {

                PagedResponse<AccountResponse> accounts = accountService.searchAccounts(request);

                ApiResponse<PagedResponse<AccountResponse>> response = ApiResponse
                                .<PagedResponse<AccountResponse>>builder()
                                .success(true)
                                .message("Accounts retrieved successfully")
                                .data(accounts)
                                .build();

                return ResponseEntity.ok(response);
        }

        @PutMapping("/{accountNo}/close")
        @PreAuthorize("hasAnyRole('BANKOFFICER', 'ADMIN')")
        @Operation(summary = "Close FD account", description = "Closes an active Fixed Deposit account with closure reason")
        public ResponseEntity<ApiResponse<AccountResponse>> closeAccount(
                        @Parameter(description = "FD Account number") @PathVariable String accountNo,
                        @Valid @RequestBody AccountClosureRequest request) {

                AccountResponse account = accountService.closeAccount(accountNo, request);

                ApiResponse<AccountResponse> response = ApiResponse.<AccountResponse>builder()
                                .success(true)
                                .message("Account closed successfully")
                                .data(account)
                                .build();

                return ResponseEntity.ok(response);
        }

        @PutMapping("/{accountNo}/reopen")
        @PreAuthorize("hasAnyRole('BANKOFFICER', 'ADMIN')")
        @Operation(summary = "Reopen FD account", description = "Reopens a previously closed Fixed Deposit account")
        public ResponseEntity<ApiResponse<AccountResponse>> reopenAccount(
                        @Parameter(description = "FD Account number") @PathVariable String accountNo) {

                AccountResponse account = accountService.reopenAccount(accountNo);

                ApiResponse<AccountResponse> response = ApiResponse.<AccountResponse>builder()
                                .success(true)
                                .message("Account reopened successfully")
                                .data(account)
                                .build();

                return ResponseEntity.ok(response);
        }

        @PutMapping("/{accountNo}/upgrade")
        @PreAuthorize("hasAnyRole('BANKOFFICER', 'ADMIN')")
        @Operation(summary = "Upgrade FD account", description = "Upgrades an existing FD account to a new product configuration")
        public ResponseEntity<ApiResponse<AccountResponse>> upgradeAccount(
                        @Parameter(description = "FD Account number") @PathVariable String accountNo,
                        @RequestBody Map<String, Object> request,
                        @Parameter(hidden = true) @RequestHeader("Authorization") String authToken) {

                AccountResponse account = accountService.upgradeAccount(accountNo, request, authToken);

                ApiResponse<AccountResponse> response = ApiResponse.<AccountResponse>builder()
                                .success(true)
                                .message("Account upgraded successfully")
                                .data(account)
                                .build();

                return ResponseEntity.ok(response);
        }
}
