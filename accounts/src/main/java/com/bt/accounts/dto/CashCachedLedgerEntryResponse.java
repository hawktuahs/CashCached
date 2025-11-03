package com.bt.accounts.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.bt.accounts.entity.CashCachedLedgerEntry;
import com.bt.accounts.entity.CashCachedLedgerEntry.Operation;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CashCachedLedgerEntryResponse {

    private Long id;
    private String customerId;
    private BigDecimal changeAmount;
    private BigDecimal balanceAfter;
    private Operation operation;
    private String transactionHash;
    private String reference;
    private LocalDateTime createdAt;

    public static CashCachedLedgerEntryResponse fromEntity(CashCachedLedgerEntry entry) {
        CashCachedLedgerEntryResponse response = new CashCachedLedgerEntryResponse();
        response.setId(entry.getId());
        response.setCustomerId(entry.getCustomerId());
        response.setChangeAmount(entry.getChangeAmount());
        response.setBalanceAfter(entry.getBalanceAfter());
        response.setOperation(entry.getOperation());
        response.setTransactionHash(entry.getTransactionHash());
        response.setReference(entry.getReference());
        response.setCreatedAt(entry.getCreatedAt());
        return response;
    }
}
