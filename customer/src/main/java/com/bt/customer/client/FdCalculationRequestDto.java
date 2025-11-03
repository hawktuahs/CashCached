package com.bt.customer.client;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FdCalculationRequestDto {
    private Long customerId;
    private String productCode;
    private BigDecimal principalAmount;
    private Integer tenureMonths;
    private Integer compoundingFrequency;
}
