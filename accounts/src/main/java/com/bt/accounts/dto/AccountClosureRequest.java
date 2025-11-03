package com.bt.accounts.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountClosureRequest {

    @NotBlank(message = "Closure reason is required")
    @Size(min = 10, max = 500, message = "Closure reason must be between 10 and 500 characters")
    private String closureReason;

    @Size(max = 1000, message = "Remarks must not exceed 1000 characters")
    private String remarks;
}
