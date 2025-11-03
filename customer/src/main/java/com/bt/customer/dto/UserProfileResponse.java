package com.bt.customer.dto;

import com.bt.customer.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "User profile information")
public class UserProfileResponse {

    @Schema(description = "User ID", example = "1")
    private Long id;

    @Schema(description = "Username", example = "john_doe")
    private String username;

    @Schema(description = "Full name", example = "John Doe")
    private String fullName;

    @Schema(description = "Email address", example = "john.doe@example.com")
    private String email;

    @Schema(description = "Phone number", example = "+1234567890")
    private String phoneNumber;

    @Schema(description = "User role", example = "CUSTOMER")
    private String role;

    @Schema(description = "Account active status", example = "true")
    private Boolean active;

    @Schema(description = "Preferred fiat currency for CashCached conversions", example = "KWD")
    private String preferredCurrency;

    @Schema(description = "Account creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;

    public static UserProfileResponse fromUser(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole().name())
                .active(user.getActive())
                .preferredCurrency(user.getPreferredCurrency())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
