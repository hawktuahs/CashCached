package com.bt.product.client;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceHealthDto {
    private String serviceName;
    private String status;
    private Boolean customerServiceAvailable;
    private Boolean productServiceAvailable;
    private String databaseStatus;
    private Long timestamp;
}
