package com.bt.accounts.dto;

import com.bt.accounts.entity.FdAccount;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountResponse {

    private Long id;
    private String accountNo;
    private String customerId;
    private String productCode;
    private BigDecimal principalAmount;
    private BigDecimal interestRate;
    private Integer tenureMonths;
    private BigDecimal maturityAmount;
    private LocalDateTime maturityDate;
    private String branchCode;
    private String status;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime closedAt;
    private String closedBy;
    private String closureReason;
    private LocalDateTime updatedAt;
    private BigDecimal currentBalance;

    public static AccountResponse fromEntity(FdAccount account) {
        return AccountResponse.builder()
                .id(account.getId())
                .accountNo(account.getAccountNo())
                .customerId(account.getCustomerId())
                .productCode(account.getProductCode())
                .principalAmount(account.getPrincipalAmount())
                .interestRate(account.getInterestRate())
                .tenureMonths(account.getTenureMonths())
                .maturityAmount(account.getMaturityAmount())
                .maturityDate(account.getMaturityDate())
                .branchCode(account.getBranchCode())
                .status(account.getStatus() != null ? account.getStatus().name() : null)
                .createdAt(account.getCreatedAt())
                .createdBy(account.getCreatedBy())
                .closedAt(account.getClosedAt())
                .closedBy(account.getClosedBy())
                .closureReason(account.getClosureReason())
                .updatedAt(account.getUpdatedAt())
                .build();
    }
}
