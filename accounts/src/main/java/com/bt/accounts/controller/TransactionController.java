package com.bt.accounts.controller;

import com.bt.accounts.dto.ApiResponse;
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
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getAccountTransactions(
            @Parameter(description = "FD Account number") @PathVariable String accountNo) {

        List<TransactionResponse> transactions = transactionService.getAccountTransactions(accountNo);

        ApiResponse<List<TransactionResponse>> response = ApiResponse.<List<TransactionResponse>>builder()
                .success(true)
                .message("Transactions retrieved successfully")
                .data(transactions)
                .build();

        return ResponseEntity.ok(response);
    }
}
