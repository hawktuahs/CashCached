package com.bt.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Status response for health checks")
public class StatusResponse {

    @Schema(description = "Service status", example = "OPERATIONAL")
    private String status;

    @Schema(description = "Authenticated user role", example = "CUSTOMER")
    private String role;

    @Schema(description = "Authenticated user email", example = "john@example.com")
    private String email;

    @Schema(description = "Response timestamp")
    private LocalDateTime timestamp;

    @Schema(description = "Service message", example = "Customer service is running")
    private String message;
}
