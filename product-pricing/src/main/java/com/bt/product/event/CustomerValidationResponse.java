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
public class CustomerValidationResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private String requestId;
    private Long customerId;
    private Boolean valid;
    private Boolean active;
    private String error;
    private LocalDateTime timestamp;
}
