package com.bt.fixeddeposit.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FdCalculationResponseEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private String requestId;
    private Long calculationId;
    private Long customerId;
    private String productCode;
    private BigDecimal principalAmount;
    private BigDecimal maturityAmount;
    private BigDecimal interestEarned;
    private BigDecimal effectiveRate;
    private Integer tenureMonths;
    private String error;
    private LocalDateTime timestamp;
}
