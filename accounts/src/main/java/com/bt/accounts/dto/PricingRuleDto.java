package com.bt.accounts.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricingRuleDto {

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
