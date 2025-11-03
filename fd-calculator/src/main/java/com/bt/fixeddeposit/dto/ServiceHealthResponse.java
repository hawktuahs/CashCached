package com.bt.fixeddeposit.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceHealthResponse {

    private String serviceName;
    private String status;
    private Boolean customerServiceAvailable;
    private Boolean productServiceAvailable;
    private String databaseStatus;
    private Long timestamp;
}
