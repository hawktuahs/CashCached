package com.bt.product.dto;

import com.bt.product.entity.Currency;
import com.bt.product.entity.ProductStatus;
import com.bt.product.entity.ProductType;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {

    private Long id;
    private String productCode;
    private String productName;
    private ProductType productType;
    private String description;
    private BigDecimal minInterestRate;
    private BigDecimal maxInterestRate;
    private Integer minTermMonths;
    private Integer maxTermMonths;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private Currency currency;
    private ProductStatus status;
    private LocalDate effectiveDate;
    private LocalDate expiryDate;
    private String regulatoryCode;
    private Boolean requiresApproval;
    private String compoundingFrequency;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
