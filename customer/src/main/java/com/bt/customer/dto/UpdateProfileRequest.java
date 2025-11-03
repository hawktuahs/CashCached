package com.bt.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request payload for updating user profile")
public class UpdateProfileRequest {

    @Size(max = 100, message = "Full name cannot exceed 100 characters")
    @Schema(description = "Updated full name", example = "John Updated Doe")
    private String fullName;

    @Email(message = "Email must be valid")
    @Schema(description = "Updated email address", example = "john.updated@example.com")
    private String email;

    @Size(max = 15, message = "Phone number cannot exceed 15 characters")
    @Schema(description = "Updated phone number", example = "+9876543210")
    private String phoneNumber;

    @Size(max = 10, message = "Currency code cannot exceed 10 characters")
    @Schema(description = "Preferred fiat currency for CashCached conversions", example = "USD")
    private String preferredCurrency;
}
