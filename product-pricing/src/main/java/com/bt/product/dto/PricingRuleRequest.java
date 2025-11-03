package com.bt.product.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricingRuleRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotBlank(message = "Rule name is required")
    @Size(max = 100, message = "Rule name must not exceed 100 characters")
    private String ruleName;

    @Size(max = 5000, message = "Rule description must not exceed 5000 characters")
    private String ruleDescription;

    @DecimalMin(value = "0.0", message = "Minimum threshold must be non-negative")
    private BigDecimal minThreshold;

    @DecimalMin(value = "0.0", message = "Maximum threshold must be non-negative")
    private BigDecimal maxThreshold;

    @DecimalMin(value = "0.0", message = "Interest rate must be non-negative")
    @DecimalMax(value = "100.0", message = "Interest rate cannot exceed 100")
    private BigDecimal interestRate;

    @DecimalMin(value = "0.0", message = "Fee amount must be non-negative")
    private BigDecimal feeAmount;

    @DecimalMin(value = "0.0", message = "Discount percentage must be non-negative")
    @DecimalMax(value = "100.0", message = "Discount percentage cannot exceed 100")
    private BigDecimal discountPercentage;

    @Min(value = 0, message = "Priority order must be non-negative")
    private Integer priorityOrder;

    private Boolean isActive;
}
