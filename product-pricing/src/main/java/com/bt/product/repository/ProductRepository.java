package com.bt.product.repository;

import com.bt.product.entity.Product;
import com.bt.product.entity.ProductStatus;
import com.bt.product.entity.ProductType;
import com.bt.product.entity.Currency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByProductCode(String productCode);

    boolean existsByProductCode(String productCode);

    List<Product> findByProductType(ProductType productType);

    List<Product> findByStatus(ProductStatus status);

    List<Product> findByCurrency(Currency currency);

    @Query("SELECT p FROM Product p WHERE p.effectiveDate <= :currentDate AND (p.expiryDate IS NULL OR p.expiryDate >= :currentDate)")
    List<Product> findActiveProductsByDate(@Param("currentDate") LocalDate currentDate);

    @Query("SELECT p FROM Product p WHERE " +
            "(:productType IS NULL OR p.productType = :productType) AND " +
            "(:currency IS NULL OR p.currency = :currency) AND " +
            "(:status IS NULL OR p.status = :status) AND " +
            "(:effectiveDate IS NULL OR p.effectiveDate <= :effectiveDate) AND " +
            "(:expiryDate IS NULL OR p.expiryDate IS NULL OR p.expiryDate >= :expiryDate)")
    List<Product> searchProducts(@Param("productType") ProductType productType,
            @Param("currency") Currency currency,
            @Param("status") ProductStatus status,
            @Param("effectiveDate") LocalDate effectiveDate,
            @Param("expiryDate") LocalDate expiryDate);

    @Query("SELECT p FROM Product p WHERE LOWER(p.productName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.productCode) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Product> searchByKeyword(@Param("keyword") String keyword);

    List<Product> findByRequiresApprovalTrue();

    @Query("SELECT COUNT(p) FROM Product p WHERE p.status = :status")
    long countByStatus(@Param("status") ProductStatus status);
}
