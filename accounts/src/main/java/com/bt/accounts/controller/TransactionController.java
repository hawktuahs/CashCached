package com.bt.accounts.controller;

import com.bt.accounts.dto.ApiResponse;
import com.bt.accounts.dto.CashCachedAccountTransferRequest;
import com.bt.accounts.dto.TransactionRequest;
import com.bt.accounts.dto.TransactionResponse;
import com.bt.accounts.service.TransactionService;
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
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Tag(name = "Transaction Management", description = "APIs for managing account transactions")
@SecurityRequirement(name = "Bearer Authentication")
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/{accountNo}/transactions")
    @PreAuthorize("hasAnyRole('BANKOFFICER', 'ADMIN')")
    @Operation(summary = "Record transaction", description = "Records a transaction (deposit, withdrawal, interest credit, etc.) for an account")
    public ResponseEntity<ApiResponse<TransactionResponse>> recordTransaction(
            @Parameter(description = "FD Account number") @PathVariable String accountNo,
            @Valid @RequestBody TransactionRequest request) {

        TransactionResponse transaction = transactionService.recordTransaction(accountNo, request);

        ApiResponse<TransactionResponse> response = ApiResponse.<TransactionResponse>builder()
                .success(true)
                .message("Transaction recorded successfully")
                .data(transaction)
                .build();

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{accountNo}/transactions")
    @Operation(summary = "Get account transactions", description = "Retrieves all transactions for a specific account ordered by date")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getTransactions(
            @Parameter(description = "FD Account number") @PathVariable String accountNo) {

        List<TransactionResponse> transactions = transactionService.getAccountTransactions(accountNo);
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }

    @PostMapping("/{accountNo}/transactions/self")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Record self transaction", description = "Customers can record deposit/withdrawal on their own account")
    public ResponseEntity<ApiResponse<TransactionResponse>> recordSelfTransaction(
            @Parameter(description = "FD Account number") @PathVariable String accountNo,
            @Valid @RequestBody TransactionRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        TransactionResponse transaction = transactionService.recordSelfTransaction(accountNo, request, userIdHeader, authHeader);

        ApiResponse<TransactionResponse> response = ApiResponse.<TransactionResponse>builder()
                .success(true)
                .message("Transaction recorded successfully")
                .data(transaction)
                .build();

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/{accountNo}/wallet/deposit")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Deposit CashCached to FD", description = "Moves tokens from customer's wallet into the specified FD account")
    public ResponseEntity<ApiResponse<TransactionResponse>> depositFromWallet(
            @PathVariable String accountNo,
            @Valid @RequestBody CashCachedAccountTransferRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        TransactionRequest txnRequest = TransactionRequest.builder()
                .transactionType("DEPOSIT")
                .amount(request.getAmount())
                .description(request.getDescription())
                .referenceNo(request.getReference())
                .remarks(request.getRemarks())
                .build();

        TransactionResponse transaction = transactionService.recordSelfTransaction(accountNo, txnRequest, userIdHeader, authHeader);

        ApiResponse<TransactionResponse> response = ApiResponse.<TransactionResponse>builder()
                .success(true)
                .message("Deposit recorded successfully")
                .data(transaction)
                .build();

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/{accountNo}/wallet/withdraw")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Withdraw CashCached from FD", description = "Moves tokens from the FD account back to the customer's wallet")
    public ResponseEntity<ApiResponse<TransactionResponse>> withdrawToWallet(
            @PathVariable String accountNo,
            @Valid @RequestBody CashCachedAccountTransferRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        TransactionRequest txnRequest = TransactionRequest.builder()
                .transactionType("WITHDRAWAL")
                .amount(request.getAmount())
                .description(request.getDescription())
                .referenceNo(request.getReference())
                .remarks(request.getRemarks())
                .build();

        TransactionResponse transaction = transactionService.recordSelfTransaction(accountNo, txnRequest, userIdHeader, authHeader);

        ApiResponse<TransactionResponse> response = ApiResponse.<TransactionResponse>builder()
                .success(true)
                .message("Withdrawal recorded successfully")
                .data(transaction)
                .build();

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}
