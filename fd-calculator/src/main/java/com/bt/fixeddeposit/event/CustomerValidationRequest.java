package com.bt.fixeddeposit.event;

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
public class CustomerValidationRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long customerId;
    private String requestId;
    private LocalDateTime timestamp;
}
