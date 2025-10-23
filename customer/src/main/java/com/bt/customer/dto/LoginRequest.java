package com.bt.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request payload for user authentication")
public class LoginRequest {

    @NotBlank(message = "Username is required")
    @Schema(description = "Username for authentication", example = "john_doe")
    private String username;

    @NotBlank(message = "Password is required")
    @Schema(description = "User password", example = "SecurePass123!")
    private String password;
}
