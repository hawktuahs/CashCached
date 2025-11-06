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
public class RedemptionResponse {

    private String accountNo;
    private String customerId;
    private String redemptionType;
    private String transactionId;
    private LocalDateTime redemptionDate;

    private BigDecimal principalAmount;
    private BigDecimal interestAmount;
    private BigDecimal penaltyAmount;
    private BigDecimal netPayoutAmount;

    private String blockchainTransactionHash;
    private String walletAddress;
    private BigDecimal newWalletBalance;

    private String status;
    private String message;
}
