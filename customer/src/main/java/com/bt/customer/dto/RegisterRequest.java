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

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    @Schema(description = "User password (will be encrypted)", example = "SecurePass123!")
    private String password;

    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Schema(description = "Optional username. If omitted, it will be auto-generated")
    private String username;

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

    @Size(max = 500, message = "Address cannot exceed 500 characters")
    @Schema(description = "Residential address", example = "123 Main St, City, Country")
    private String address;

    @Size(min = 12, max = 12, message = "Aadhaar must be 12 digits")
    @Schema(description = "12-digit Aadhaar number", example = "123456789012")
    private String aadhaarNumber;

    @Size(min = 10, max = 10, message = "PAN must be 10 characters")
    @Schema(description = "10-character PAN number", example = "ABCDE1234F")
    private String panNumber;

    @Schema(description = "Date of birth (ISO 8601 format)", example = "1990-01-15")
    private java.time.LocalDate dateOfBirth;

    @Size(max = 10, message = "Currency code cannot exceed 10 characters")
    @Schema(description = "Preferred currency code", example = "KWD")
    private String preferredCurrency;

    @Schema(description = "User role (defaults to CUSTOMER)", example = "CUSTOMER", allowableValues = { "CUSTOMER",
            "ADMIN", "BANKOFFICER" })
    private User.Role role;
}
