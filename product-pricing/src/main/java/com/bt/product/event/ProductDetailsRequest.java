package com.bt.product.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDetailsRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String productCode;
    private Long productId;
    private String requestId;
    private LocalDateTime timestamp;
}
