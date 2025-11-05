package com.bt.product.dto;

import com.bt.product.entity.Currency;
import com.bt.product.entity.ProductStatus;
import com.bt.product.entity.ProductType;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRequest {

    @NotBlank(message = "Product code is required")
    @Size(min = 3, max = 50, message = "Product code must be between 3 and 50 characters")
    @Pattern(regexp = "^[A-Z0-9_-]+$", message = "Product code must contain only uppercase letters, numbers, hyphens, and underscores")
    private String productCode;

    @NotBlank(message = "Product name is required")
    @Size(max = 200, message = "Product name must not exceed 200 characters")
    private String productName;

    @NotNull(message = "Product type is required")
    private ProductType productType;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    @DecimalMin(value = "0.0", message = "Minimum interest rate must be non-negative")
    @DecimalMax(value = "100.0", message = "Minimum interest rate cannot exceed 100")
    private BigDecimal minInterestRate;

    @DecimalMin(value = "0.0", message = "Maximum interest rate must be non-negative")
    @DecimalMax(value = "100.0", message = "Maximum interest rate cannot exceed 100")
    private BigDecimal maxInterestRate;

    @Min(value = 1, message = "Minimum term must be at least 1 month")
    private Integer minTermMonths;

    @Min(value = 1, message = "Maximum term must be at least 1 month")
    private Integer maxTermMonths;

    @DecimalMin(value = "0.0", message = "Minimum amount must be non-negative")
    private BigDecimal minAmount;

    @DecimalMin(value = "0.0", message = "Maximum amount must be non-negative")
    private BigDecimal maxAmount;

    @NotNull(message = "Currency is required")
    private Currency currency;

    private ProductStatus status;

    @NotNull(message = "Effective date is required")
    private LocalDate effectiveDate;

    private LocalDate expiryDate;

    @Size(max = 100, message = "Regulatory code must not exceed 100 characters")
    private String regulatoryCode;

    private Boolean requiresApproval;

    @Size(max = 20, message = "Compounding frequency must not exceed 20 characters")
    private String compoundingFrequency;

    @DecimalMin(value = "0.0", message = "Premature penalty rate must be non-negative")
    @DecimalMax(value = "1.0", message = "Premature penalty rate cannot exceed 100% of balance")
    private BigDecimal prematurePenaltyRate;

    @Min(value = 0, message = "Premature penalty grace days must be non-negative")
    @Max(value = 365, message = "Premature penalty grace days cannot exceed 365")
    private Integer prematurePenaltyGraceDays;
}
