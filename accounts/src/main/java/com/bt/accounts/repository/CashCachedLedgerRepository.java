package com.bt.accounts.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.bt.accounts.entity.CashCachedLedgerEntry;

public interface CashCachedLedgerRepository extends JpaRepository<CashCachedLedgerEntry, Long> {

    List<CashCachedLedgerEntry> findByCustomerIdOrderByCreatedAtDesc(String customerId);

    List<CashCachedLedgerEntry> findAllByOrderByCreatedAtDesc();

    Page<CashCachedLedgerEntry> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
