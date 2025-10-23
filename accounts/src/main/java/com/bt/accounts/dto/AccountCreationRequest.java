package com.bt.accounts.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountCreationRequest {

    @NotBlank(message = "Customer ID is required")
    @Size(max = 50, message = "Customer ID must not exceed 50 characters")
    private String customerId;

    @NotBlank(message = "Product code is required")
    @Size(max = 50, message = "Product code must not exceed 50 characters")
    private String productCode;

    @NotNull(message = "Principal amount is required")
    @DecimalMin(value = "1000.00", message = "Minimum principal amount is 1000")
    @DecimalMax(value = "100000000.00", message = "Maximum principal amount is 100000000")
    private BigDecimal principalAmount;

    @NotNull(message = "Interest rate is required")
    @DecimalMin(value = "0.01", message = "Interest rate must be greater than 0")
    @DecimalMax(value = "20.00", message = "Interest rate cannot exceed 20%")
    private BigDecimal interestRate;

    @NotNull(message = "Tenure is required")
    @Min(value = 1, message = "Minimum tenure is 1 month")
    @Max(value = 120, message = "Maximum tenure is 120 months")
    private Integer tenureMonths;

    @NotBlank(message = "Branch code is required")
    @Size(max = 20, message = "Branch code must not exceed 20 characters")
    @Pattern(regexp = "^[A-Z0-9]{3,20}$", message = "Branch code must be alphanumeric uppercase")
    private String branchCode;

    @Size(max = 500, message = "Remarks must not exceed 500 characters")
    private String remarks;
}
