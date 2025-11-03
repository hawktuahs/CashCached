package com.bt.accounts.repository;

import com.bt.accounts.entity.FdAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface FdAccountRepository extends JpaRepository<FdAccount, Long> {

    Optional<FdAccount> findByAccountNo(String accountNo);

    List<FdAccount> findByCustomerId(String customerId);

    List<FdAccount> findByCustomerIdAndStatus(String customerId, FdAccount.AccountStatus status);

    List<FdAccount> findByBranchCode(String branchCode);

    List<FdAccount> findByStatus(FdAccount.AccountStatus status);

    @Query("SELECT COUNT(a) FROM FdAccount a WHERE a.branchCode = :branchCode AND a.createdAt >= CURRENT_DATE")
    Long countTodayAccountsByBranch(@Param("branchCode") String branchCode);

    @Query("SELECT a FROM FdAccount a WHERE a.customerId = :customerId ORDER BY a.createdAt DESC")
    List<FdAccount> findAllByCustomerIdOrderByCreatedAtDesc(@Param("customerId") String customerId);

    @Query("SELECT COUNT(a) FROM FdAccount a WHERE a.status = :status")
    Long countByStatus(@Param("status") FdAccount.AccountStatus status);

    @Query("SELECT a FROM FdAccount a WHERE " +
            "(:customerId IS NULL OR a.customerId = :customerId) AND " +
            "(:productCode IS NULL OR a.productCode = :productCode) AND " +
            "(:status IS NULL OR a.status = :status) AND " +
            "(:branchCode IS NULL OR a.branchCode = :branchCode)")
    Page<FdAccount> searchAccounts(
            @Param("customerId") String customerId,
            @Param("productCode") String productCode,
            @Param("status") FdAccount.AccountStatus status,
            @Param("branchCode") String branchCode,
            Pageable pageable);

    boolean existsByAccountNo(String accountNo);
}
