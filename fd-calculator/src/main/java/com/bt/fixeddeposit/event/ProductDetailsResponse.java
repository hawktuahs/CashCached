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
public class ProductDetailsResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private String requestId;
    private Long productId;
    private String productCode;
    private String productName;
    private String status;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private Integer minTermMonths;
    private Integer maxTermMonths;
    private BigDecimal minInterestRate;
    private BigDecimal maxInterestRate;
    private String currency;
    private String compoundingFrequency;
    private String error;
    private LocalDateTime timestamp;
}
