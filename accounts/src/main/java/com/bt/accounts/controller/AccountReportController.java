package com.bt.accounts.controller;

import com.bt.accounts.dto.ApiResponse;
import com.bt.accounts.entity.FdAccount.AccountStatus;
import com.bt.accounts.service.AccountReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/v1/accounts/reports")
@RequiredArgsConstructor
@Tag(name = "Account Reports", description = "Admin-only endpoints for generating account reports")
@SecurityRequirement(name = "Bearer Authentication")
public class AccountReportController {

    private final AccountReportService accountReportService;

    @GetMapping("/export-csv")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Export accounts as CSV", description = "Generates a CSV dump of all accounts with optional filters. Admin only.")
    public ResponseEntity<byte[]> exportAccountsCsv(
            @Parameter(description = "Filter by customer ID") @RequestParam(required = false) String customerId,

            @Parameter(description = "Filter by product code") @RequestParam(required = false) String productCode,

            @Parameter(description = "Filter by status") @RequestParam(required = false) AccountStatus status,

            @Parameter(description = "Filter by created date from (yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdDateFrom,

            @Parameter(description = "Filter by created date to (yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdDateTo,

            @Parameter(description = "Filter by maturity date from (yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate maturityDateFrom,

            @Parameter(description = "Filter by maturity date to (yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate maturityDateTo) {

        String csvContent = accountReportService.generateAccountReportCsv(
                customerId, productCode, status,
                createdDateFrom, createdDateTo,
                maturityDateFrom, maturityDateTo);

        LocalDate now = LocalDate.now();
        String filename = String.format("accounts-report-%s.csv",
                DateTimeFormatter.ISO_DATE.format(now));

        return ResponseEntity
                .status(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvContent.getBytes());
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get account report summary", description = "Returns a summary of accounts based on filters. Admin only.")
    public ResponseEntity<ApiResponse<String>> getAccountSummary(
            @Parameter(description = "Filter by customer ID") @RequestParam(required = false) String customerId,

            @Parameter(description = "Filter by product code") @RequestParam(required = false) String productCode,

            @Parameter(description = "Filter by status") @RequestParam(required = false) AccountStatus status) {

        return ResponseEntity.ok(ApiResponse.<String>builder()
                .success(true)
                .message("Use /export-csv endpoint to get detailed account report")
                .build());
    }
}
