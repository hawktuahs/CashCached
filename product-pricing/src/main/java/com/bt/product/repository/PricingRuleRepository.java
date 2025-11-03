package com.bt.product.repository;

import com.bt.product.entity.PricingRule;
import com.bt.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PricingRuleRepository extends JpaRepository<PricingRule, Long> {

    List<PricingRule> findByProduct(Product product);

    List<PricingRule> findByProductId(Long productId);

    List<PricingRule> findByIsActiveTrue();

    @Query("SELECT pr FROM PricingRule pr WHERE pr.product.id = :productId AND pr.isActive = true ORDER BY pr.priorityOrder ASC")
    List<PricingRule> findActiveRulesByProductIdOrderByPriority(@Param("productId") Long productId);

    @Query("SELECT pr FROM PricingRule pr WHERE pr.product.id = :productId AND pr.isActive = true AND " +
            "(pr.minThreshold IS NULL OR pr.minThreshold <= :amount) AND " +
            "(pr.maxThreshold IS NULL OR pr.maxThreshold >= :amount) " +
            "ORDER BY pr.priorityOrder ASC")
    List<PricingRule> findApplicableRulesByAmount(@Param("productId") Long productId,
            @Param("amount") BigDecimal amount);

    @Query("SELECT pr FROM PricingRule pr WHERE pr.product.productCode = :productCode AND pr.isActive = true ORDER BY pr.priorityOrder ASC")
    List<PricingRule> findActiveRulesByProductCode(@Param("productCode") String productCode);

    long countByProductId(Long productId);
}
