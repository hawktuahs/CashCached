package com.bt.fixeddeposit.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "fd_calculations", indexes = {
        @Index(name = "idx_customer_id", columnList = "customer_id"),
        @Index(name = "idx_product_code", columnList = "product_code"),
        @Index(name = "idx_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FdCalculation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "product_code", nullable = false, length = 50)
    private String productCode;

    @Column(name = "principal_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal principalAmount;

    @Column(name = "tenure_months", nullable = false)
    private Integer tenureMonths;

    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal interestRate;

    @Column(name = "compounding_frequency", nullable = false)
    private Integer compoundingFrequency;

    @Column(name = "maturity_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal maturityAmount;

    @Column(name = "interest_earned", nullable = false, precision = 15, scale = 2)
    private BigDecimal interestEarned;

    @Column(name = "effective_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal effectiveRate;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "calculation_date", nullable = false)
    private LocalDateTime calculationDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.calculationDate = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
