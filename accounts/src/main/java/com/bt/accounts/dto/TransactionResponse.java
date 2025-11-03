package com.bt.accounts.dto;

import com.bt.accounts.entity.AccountTransaction;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {

    private Long id;
    private String transactionId;
    private String accountNo;
    private String transactionType;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String description;
    private String referenceNo;
    private LocalDateTime transactionDate;
    private String processedBy;
    private String remarks;

    public static TransactionResponse fromEntity(AccountTransaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .transactionId(transaction.getTransactionId())
                .accountNo(transaction.getAccountNo())
                .transactionType(
                        transaction.getTransactionType() != null ? transaction.getTransactionType().name() : null)
                .amount(transaction.getAmount())
                .balanceAfter(transaction.getBalanceAfter())
                .description(transaction.getDescription())
                .referenceNo(transaction.getReferenceNo())
                .transactionDate(transaction.getTransactionDate())
                .processedBy(transaction.getProcessedBy())
                .remarks(transaction.getRemarks())
                .build();
    }
}
