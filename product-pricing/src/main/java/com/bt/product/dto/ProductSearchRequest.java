package com.bt.product.dto;

import com.bt.product.entity.Currency;
import com.bt.product.entity.ProductStatus;
import com.bt.product.entity.ProductType;
import lombok.*;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSearchRequest {

    private ProductType productType;
    private Currency currency;
    private ProductStatus status;
    private LocalDate effectiveDate;
    private LocalDate expiryDate;
    private String productCode;
    private String productName;
}
