package com.bt.accounts.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bt.accounts.entity.CashCachedWallet;

public interface CashCachedWalletRepository extends JpaRepository<CashCachedWallet, Long> {

    Optional<CashCachedWallet> findByCustomerId(String customerId);

    boolean existsByCustomerId(String customerId);
}
