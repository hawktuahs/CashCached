package com.bt.accounts.event;

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
public class AccountRedemptionEvent {

    private String eventId;
    private String eventType;
    private LocalDateTime timestamp;

    private String accountNo;
    private String customerId;
    private String productCode;

    private String redemptionType;
    private BigDecimal principalAmount;
    private BigDecimal interestAmount;
    private BigDecimal penaltyAmount;
    private BigDecimal netPayoutAmount;

    private String transactionId;
    private String blockchainTransactionHash;
    private String walletAddress;

    private LocalDateTime maturityDate;
    private Integer daysBeforeMaturity;
    private String reason;
    private String processedBy;
}
