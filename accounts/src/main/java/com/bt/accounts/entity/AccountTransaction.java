package com.bt.accounts.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.bt.accounts.time.TimeProvider;

@Entity
@Table(name = "account_transactions", indexes = {
        @Index(name = "idx_account_no", columnList = "account_no"),
        @Index(name = "idx_transaction_date", columnList = "transaction_date"),
        @Index(name = "idx_transaction_type", columnList = "transaction_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false, unique = true, length = 100)
    private String transactionId;

    @Column(name = "account_no", nullable = false, length = 50)
    private String accountNo;

    @Column(name = "transaction_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance_after", precision = 15, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "reference_no", length = 100)
    private String referenceNo;

    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    @Column(name = "processed_by", length = 100)
    private String processedBy;

    @Column(name = "remarks", length = 1000)
    private String remarks;

    @PrePersist
    protected void onCreate() {
        if (transactionDate == null) {
            transactionDate = TimeProvider.currentDateTime();
        }
    }

    public enum TransactionType {
        DEPOSIT,
        WITHDRAWAL,
        INTEREST_CREDIT,
        PREMATURE_CLOSURE,
        MATURITY_PAYOUT,
        PENALTY_DEBIT,
        REVERSAL
    }
}
