package com.bt.accounts.repository;

import com.bt.accounts.entity.AccountTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountTransactionRepository extends JpaRepository<AccountTransaction, Long> {

    List<AccountTransaction> findByAccountNo(String accountNo);

    List<AccountTransaction> findByAccountNoOrderByTransactionDateDesc(String accountNo);

    Optional<AccountTransaction> findTopByAccountNoAndTransactionTypeOrderByTransactionDateDesc(
            String accountNo, AccountTransaction.TransactionType transactionType);

    List<AccountTransaction> findByAccountNoAndTransactionTypeOrderByTransactionDateDesc(
            String accountNo, AccountTransaction.TransactionType transactionType);

    Optional<AccountTransaction> findByTransactionId(String transactionId);

    List<AccountTransaction> findByTransactionType(AccountTransaction.TransactionType transactionType);

    @Query("SELECT t FROM AccountTransaction t WHERE t.accountNo = :accountNo " +
            "AND t.transactionDate BETWEEN :startDate AND :endDate " +
            "ORDER BY t.transactionDate DESC")
    List<AccountTransaction> findByAccountNoAndDateRange(
            @Param("accountNo") String accountNo,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(t) FROM AccountTransaction t WHERE t.accountNo = :accountNo")
    Long countByAccountNo(@Param("accountNo") String accountNo);

    boolean existsByTransactionId(String transactionId);
}
