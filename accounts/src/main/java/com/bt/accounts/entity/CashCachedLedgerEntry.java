package com.bt.accounts.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.bt.accounts.time.TimeProvider;

@Entity
@Table(name = "cashcached_ledger", indexes = {
        @Index(name = "idx_cashcached_customer", columnList = "customer_id"),
        @Index(name = "idx_cashcached_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashCachedLedgerEntry {

    public enum Operation {
        ISSUE,
        TRANSFER_IN,
        TRANSFER_OUT,
        REDEEM,
        CONTRACT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false, length = 64)
    private String customerId;

    @Column(name = "change_amount", nullable = false, precision = 38, scale = 18)
    private BigDecimal changeAmount;

    @Column(name = "balance_after", nullable = false, precision = 38, scale = 18)
    private BigDecimal balanceAfter;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation", nullable = false, length = 32)
    private Operation operation;

    @Column(name = "transaction_hash", length = 80)
    private String transactionHash;

    @Column(name = "reference", length = 255)
    private String reference;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void assignTimestamp() {
        createdAt = TimeProvider.currentDateTime();
    }
}
