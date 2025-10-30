package com.bt.accounts.event;

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
public class FdCalculationRequestEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private String requestId;
    private Long customerId;
    private String productCode;
    private BigDecimal principalAmount;
    private Integer tenureMonths;
    private LocalDateTime timestamp;
}
