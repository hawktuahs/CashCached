package com.bt.accounts.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedemptionEnquiryResponse {

    private String accountNo;
    private String customerId;
    private String productCode;
    private BigDecimal principalAmount;
    private BigDecimal interestRate;
    private Integer tenureMonths;
    private LocalDateTime maturityDate;
    private LocalDateTime currentDate;
    private Boolean isMatured;
    private Integer daysUntilMaturity;
    private Integer daysOverdue;

    private BigDecimal accruedInterest;
    private BigDecimal currentBalance;
    private BigDecimal maturityAmount;
    private BigDecimal penaltyAmount;
    private BigDecimal netPayableAmount;
    private String penaltyReason;

    private BigDecimal currentWalletBalance;
    private Boolean hasSufficientBalance;
    private String redemptionEligibility;
    private String[] warnings;
}
