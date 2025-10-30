package com.bt.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductReportDto {
    private String productCode;
    private String productName;
    private String productType;
    private String description;
    private BigDecimal minInterestRate;
    private BigDecimal maxInterestRate;
    private Integer minTermMonths;
    private Integer maxTermMonths;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private String currency;
    private String status;
    private LocalDate effectiveDate;
    private LocalDate expiryDate;
    private String regulatoryCode;
    private Boolean requiresApproval;
    private String compoundingFrequency;
    private LocalDate createdAt;
    private LocalDate updatedAt;
}
