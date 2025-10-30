package com.bt.product.controller;

import com.bt.product.dto.ApiResponse;
import com.bt.product.entity.Currency;
import com.bt.product.entity.ProductStatus;
import com.bt.product.entity.ProductType;
import com.bt.product.service.ProductReportService;
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
@RequestMapping("/api/v1/product/reports")
@RequiredArgsConstructor
@Tag(name = "Product Reports", description = "Admin-only endpoints for generating product reports")
@SecurityRequirement(name = "Bearer Authentication")
public class ProductReportController {

    private final ProductReportService productReportService;

    @GetMapping("/export-csv")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Export products as CSV", description = "Generates a CSV dump of all products with optional filters. Admin only.")
    public ResponseEntity<byte[]> exportProductsCsv(
            @Parameter(description = "Filter by product type") @RequestParam(required = false) ProductType productType,

            @Parameter(description = "Filter by currency") @RequestParam(required = false) Currency currency,

            @Parameter(description = "Filter by status") @RequestParam(required = false) ProductStatus status,

            @Parameter(description = "Filter by effective date from (yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveDateFrom,

            @Parameter(description = "Filter by effective date to (yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveDateTo,

            @Parameter(description = "Filter by approval requirement") @RequestParam(required = false) Boolean requiresApproval) {

        String csvContent = productReportService.generateProductReportCsv(
                productType, currency, status,
                effectiveDateFrom, effectiveDateTo, requiresApproval);

        LocalDate now = LocalDate.now();
        String filename = String.format("products-report-%s.csv",
                DateTimeFormatter.ISO_DATE.format(now));

        return ResponseEntity
                .status(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvContent.getBytes());
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get product report summary", description = "Returns a summary of products based on filters. Admin only.")
    public ResponseEntity<ApiResponse<String>> getProductSummary(
            @Parameter(description = "Filter by product type") @RequestParam(required = false) ProductType productType,

            @Parameter(description = "Filter by currency") @RequestParam(required = false) Currency currency,

            @Parameter(description = "Filter by status") @RequestParam(required = false) ProductStatus status) {

        return ResponseEntity.ok(ApiResponse.<String>builder()
                .success(true)
                .message("Use /export-csv endpoint to get detailed product report")
                .build());
    }
}
