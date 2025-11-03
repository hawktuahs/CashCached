package com.bt.fixeddeposit.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FdCalculationRequest {

    @NotNull(message = "Customer ID is required")
    @Positive(message = "Customer ID must be positive")
    private Long customerId;

    @NotBlank(message = "Product code is required")
    @Pattern(regexp = "^[A-Z0-9-]{3,20}$", message = "Invalid product code format")
    private String productCode;

    @NotNull(message = "Principal amount is required")
    @DecimalMin(value = "1000.00", message = "Principal amount must be at least 1000")
    @DecimalMax(value = "100000000.00", message = "Principal amount cannot exceed 100000000")
    private BigDecimal principalAmount;

    @NotNull(message = "Tenure is required")
    @Min(value = 1, message = "Tenure must be at least 1 month")
    @Max(value = 120, message = "Tenure cannot exceed 120 months")
    private Integer tenureMonths;

    private Integer compoundingFrequency;
}
