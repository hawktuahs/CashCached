package com.bt.fixeddeposit.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FdHistoryResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private String requestId;
    private List<FdCalculationData> calculations;
    private String error;
    private LocalDateTime timestamp;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FdCalculationData {
        private Long id;
        private Long customerId;
        private String productCode;
        private BigDecimal principalAmount;
        private BigDecimal maturityAmount;
        private BigDecimal interestEarned;
        private Integer tenureMonths;
    }
}
