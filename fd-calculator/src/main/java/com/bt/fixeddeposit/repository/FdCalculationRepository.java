package com.bt.fixeddeposit.repository;

import com.bt.fixeddeposit.entity.FdCalculation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FdCalculationRepository extends JpaRepository<FdCalculation, Long> {

    List<FdCalculation> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<FdCalculation> findByCustomerIdAndProductCodeOrderByCreatedAtDesc(Long customerId, String productCode);

    @Query("SELECT f FROM FdCalculation f WHERE f.customerId = :customerId AND f.createdAt >= :startDate ORDER BY f.createdAt DESC")
    List<FdCalculation> findRecentCalculationsByCustomer(@Param("customerId") Long customerId,
            @Param("startDate") LocalDateTime startDate);

    @Query("SELECT f FROM FdCalculation f WHERE f.productCode = :productCode AND f.createdAt >= :startDate")
    List<FdCalculation> findCalculationsByProductAndDate(@Param("productCode") String productCode,
            @Param("startDate") LocalDateTime startDate);

    @Query("SELECT COUNT(f) FROM FdCalculation f WHERE f.customerId = :customerId")
    Long countByCustomerId(@Param("customerId") Long customerId);
}
