package com.bt.customer.controller;

import com.bt.customer.dto.ApiResponse;
import com.bt.customer.entity.User;
import com.bt.customer.service.CustomerReportService;
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
@RequestMapping("/api/v1/customer/reports")
@RequiredArgsConstructor
@Tag(name = "Customer Reports", description = "Admin-only endpoints for generating customer reports")
@SecurityRequirement(name = "Bearer Authentication")
public class CustomerReportController {

    private final CustomerReportService customerReportService;

    @GetMapping("/export-csv")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Export customers as CSV", description = "Generates a CSV dump of all customers with optional filters. Admin only.")
    public ResponseEntity<byte[]> exportCustomersCsv(
            @Parameter(description = "Filter by role") @RequestParam(required = false) User.Role role,

            @Parameter(description = "Filter by active status") @RequestParam(required = false) Boolean active,

            @Parameter(description = "Filter by created date from (yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdDateFrom,

            @Parameter(description = "Filter by created date to (yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdDateTo) {

        String csvContent = customerReportService.generateCustomerReportCsv(
                role, active, createdDateFrom, createdDateTo);

        LocalDate now = LocalDate.now();
        String filename = String.format("customers-report-%s.csv",
                DateTimeFormatter.ISO_DATE.format(now));

        return ResponseEntity
                .status(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvContent.getBytes());
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get customer report summary", description = "Returns a summary of customers based on filters. Admin only.")
    public ResponseEntity<ApiResponse<String>> getCustomerSummary(
            @Parameter(description = "Filter by role") @RequestParam(required = false) User.Role role,

            @Parameter(description = "Filter by active status") @RequestParam(required = false) Boolean active) {

        return ResponseEntity.ok(ApiResponse.<String>builder()
                .success(true)
                .message("Use /export-csv endpoint to get detailed customer report")
                .build());
    }
}
