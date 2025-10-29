package com.bt.accounts.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CashCachedAccountTransferRequest {

    @NotBlank(message = "Account number is required")
    private String accountNo;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1", message = "Amount must be at least 1 CashCached token")
    private BigDecimal amount;

    @Size(max = 500)
    private String description;

    @Size(max = 100)
    private String reference;

    @Size(max = 1000)
    private String remarks;
}
