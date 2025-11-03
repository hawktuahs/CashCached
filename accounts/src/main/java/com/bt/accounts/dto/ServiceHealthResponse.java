package com.bt.accounts.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceHealthResponse {

    private String serviceName;
    private String status;
    private String version;
    private Boolean databaseConnected;
    private Boolean customerServiceConnected;
    private Boolean productServiceConnected;
    private Boolean fdCalculatorServiceConnected;
    private String timestamp;
}
