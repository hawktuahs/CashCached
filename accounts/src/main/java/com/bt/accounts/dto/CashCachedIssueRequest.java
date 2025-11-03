package com.bt.accounts.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CashCachedIssueRequest {

    @NotBlank
    private String customerId;

    @NotNull
    @Positive
    private BigDecimal amount;

    private String reference;
}
