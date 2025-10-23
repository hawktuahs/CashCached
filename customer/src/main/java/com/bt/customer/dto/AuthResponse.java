package com.bt.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Response payload containing JWT token and authentication details")
public class AuthResponse {

    @Schema(description = "JWT authentication token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String token;

    @Schema(description = "Token type (always Bearer)", example = "Bearer")
    private String tokenType;

    @Schema(description = "Authenticated username", example = "john_doe")
    private String username;

    @Schema(description = "User role", example = "CUSTOMER")
    private String role;

    @Schema(description = "Response message", example = "Authentication successful")
    private String message;

    public AuthResponse(String token, String username, String role, String message) {
        this.token = token;
        this.tokenType = "Bearer";
        this.username = username;
        this.role = role;
        this.message = message;
    }
}
