package com.bt.customer.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Error response structure")
public class ErrorResponse {

    @Schema(description = "HTTP status code", example = "400")
    private Integer status;

    @Schema(description = "Error message", example = "Username already exists")
    private String message;

    @Schema(description = "Error timestamp")
    private LocalDateTime timestamp;

    @Schema(description = "Request path", example = "/api/auth/register")
    private String path;
}
