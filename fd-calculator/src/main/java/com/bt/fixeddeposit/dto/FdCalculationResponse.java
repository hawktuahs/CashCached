package com.bt.fixeddeposit.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FdCalculationResponse {

    private Long id;
    private Long customerId;
    private String productCode;
    private String productName;
    private BigDecimal principalAmount;
    private Integer tenureMonths;
    private BigDecimal interestRate;
    private Integer compoundingFrequency;
    private BigDecimal maturityAmount;
    private BigDecimal interestEarned;
    private BigDecimal effectiveRate;
    private String currency;
    private LocalDateTime calculationDate;
    private LocalDateTime createdAt;
}
