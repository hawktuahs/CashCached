package com.bt.accounts.dto;

import java.math.BigDecimal;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CashCachedBalanceResponse {

    private String customerId;
    private BigDecimal balance;
    private String baseCurrency;
    private BigDecimal baseValue;
    private String targetCurrency;
    private BigDecimal targetValue;
    private Map<String, BigDecimal> rates;
}
