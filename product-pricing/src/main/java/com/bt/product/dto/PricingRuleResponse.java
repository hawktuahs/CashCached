package com.bt.product.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricingRuleResponse {

    private Long id;
    private Long productId;
    private String productCode;
    private String ruleName;
    private String ruleDescription;
    private BigDecimal minThreshold;
    private BigDecimal maxThreshold;
    private BigDecimal interestRate;
    private BigDecimal feeAmount;
    private BigDecimal discountPercentage;
    private Integer priorityOrder;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
