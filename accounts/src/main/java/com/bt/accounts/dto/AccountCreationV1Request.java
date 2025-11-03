package com.bt.accounts.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountCreationV1Request {

    @NotBlank(message = "Customer ID is required")
    @Size(max = 50, message = "Customer ID must not exceed 50 characters")
    private String customerId;

    @NotBlank(message = "Product code is required")
    @Size(max = 50, message = "Product code must not exceed 50 characters")
    private String productCode;

    @NotNull(message = "Principal token amount is required")
    @DecimalMin(value = "1.00", message = "Minimum principal amount is 1 CashCached token")
    @DecimalMax(value = "100000000.00", message = "Maximum principal amount is 100000000 CashCached tokens")
    private BigDecimal principalAmount;

    @NotBlank(message = "Branch code is required")
    @Size(max = 20, message = "Branch code must not exceed 20 characters")
    @Pattern(regexp = "^[A-Z0-9]{3,20}$", message = "Branch code must be alphanumeric uppercase")
    private String branchCode;

    @Size(max = 500, message = "Remarks must not exceed 500 characters")
    private String remarks;
}
