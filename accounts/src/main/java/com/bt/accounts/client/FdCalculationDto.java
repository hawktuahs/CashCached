package com.bt.accounts.client;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FdCalculationDto {

    private Long calculationId;
    private String customerId;
    private String productCode;
    private BigDecimal principalAmount;
    private BigDecimal interestRate;
    private Integer tenureMonths;
    private BigDecimal maturityAmount;
    private BigDecimal interestEarned;
    private BigDecimal effectiveRate;
    private String calculationDate;
}
