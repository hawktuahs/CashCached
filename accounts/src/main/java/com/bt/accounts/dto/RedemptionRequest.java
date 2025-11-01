package com.bt.accounts.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedemptionRequest {

    @NotBlank(message = "Redemption type is required")
    private String redemptionType;

    @Size(max = 500, message = "Reason must not exceed 500 characters")
    private String reason;

    private Boolean acceptPenalty;

    private String beneficiaryCustomerId;
}
