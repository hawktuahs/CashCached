package com.bt.customer.dto;

import com.bt.customer.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request payload for user registration")
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Schema(description = "Unique username for authentication", example = "john_doe")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    @Schema(description = "User password (will be encrypted)", example = "SecurePass123!")
    private String password;

    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name cannot exceed 100 characters")
    @Schema(description = "Full name of the user", example = "John Doe")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Schema(description = "Email address for communication", example = "john.doe@example.com")
    private String email;

    @Size(max = 15, message = "Phone number cannot exceed 15 characters")
    @Schema(description = "Contact phone number", example = "+1234567890")
    private String phoneNumber;

    @Schema(description = "User role (defaults to CUSTOMER)", example = "CUSTOMER", allowableValues = { "CUSTOMER",
            "ADMIN", "BANKOFFICER" })
    private User.Role role;
}
