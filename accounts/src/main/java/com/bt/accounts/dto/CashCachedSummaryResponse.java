package com.bt.accounts.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CashCachedSummaryResponse {

    private String contractAddress;
    private String treasuryAddress;
    private BigDecimal ledgerTotal;
    private BigDecimal onChainSupply;
    private BigDecimal variance;
}
