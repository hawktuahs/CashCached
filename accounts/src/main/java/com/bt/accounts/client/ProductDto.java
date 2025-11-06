package com.bt.accounts.client;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDto {

    private Long id;
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
    private BigDecimal prematurePenaltyRate;
    private Integer prematurePenaltyGraceDays;
}
