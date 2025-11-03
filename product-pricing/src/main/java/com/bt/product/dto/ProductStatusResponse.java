package com.bt.product.dto;

import com.bt.product.entity.ProductStatus;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductStatusResponse {

    private String productCode;
    private String productName;
    private ProductStatus status;
    private Boolean isValid;
    private Boolean isActive;
    private LocalDate effectiveDate;
    private LocalDate expiryDate;
    private BigDecimal currentInterestRate;
    private List<PricingRuleResponse> applicablePricingRules;
    private String validationMessage;
}
