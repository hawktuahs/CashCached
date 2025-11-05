package com.bt.accounts.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.bt.accounts.time.TimeProvider;

@Entity
@Table(name = "fd_accounts", indexes = {
        @Index(name = "idx_account_no", columnList = "account_no", unique = true),
        @Index(name = "idx_customer_id", columnList = "customer_id"),
        @Index(name = "idx_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FdAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_no", nullable = false, unique = true, length = 50)
    private String accountNo;

    @Column(name = "customer_id", nullable = false, length = 50)
    private String customerId;

    @Column(name = "product_code", nullable = false, length = 50)
    private String productCode;

    @Column(name = "product_ref_id")
    private Long productRefId;

    @Column(name = "product_type", length = 50)
    private String productType;

    @Column(name = "principal_amount", nullable = false, precision = 38, scale = 18)
    private BigDecimal principalAmount;

    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal interestRate;

    @Column(name = "base_interest_rate", precision = 5, scale = 2)
    private BigDecimal baseInterestRate;

    @Column(name = "tenure_months", nullable = false)
    private Integer tenureMonths;

    @Column(name = "product_max_tenure_months")
    private Integer productMaxTenureMonths;

    @Column(name = "maturity_amount", precision = 38, scale = 18)
    private BigDecimal maturityAmount;

    @Column(name = "maturity_date")
    private LocalDateTime maturityDate;

    @Column(name = "next_payout_at")
    private LocalDateTime nextPayoutAt;

    @Column(name = "branch_code", nullable = false, length = 20)
    private String branchCode;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private AccountStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "closed_by", length = 100)
    private String closedBy;

    @Column(name = "closure_reason", length = 500)
    private String closureReason;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_interest_accrual_at")
    private LocalDateTime lastInterestAccrualAt;

    @Column(name = "next_interest_accrual_at")
    private LocalDateTime nextInterestAccrualAt;

    @Column(name = "total_interest_accrued", precision = 38, scale = 18)
    private BigDecimal totalInterestAccrued;

    @Column(name = "premature_penalty_rate", precision = 5, scale = 4)
    private BigDecimal prematurePenaltyRate;

    @Column(name = "premature_penalty_grace_days")
    private Integer prematurePenaltyGraceDays;

    @Column(name = "active_pricing_rule_id")
    private Long activePricingRuleId;

    @Column(name = "active_pricing_rule_name", length = 200)
    private String activePricingRuleName;

    @Column(name = "pricing_rule_applied_at")
    private LocalDateTime pricingRuleAppliedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = TimeProvider.currentDateTime();
        createdAt = now;
        updatedAt = now;
        if (status == null) {
            status = AccountStatus.ACTIVE;
        }
        if (totalInterestAccrued == null) {
            totalInterestAccrued = BigDecimal.ZERO;
        }
        if (baseInterestRate == null) {
            baseInterestRate = interestRate;
        }
        if (prematurePenaltyRate == null) {
            prematurePenaltyRate = BigDecimal.ZERO;
        }
        if (prematurePenaltyGraceDays == null) {
            prematurePenaltyGraceDays = 0;
        }
        if (createdAt != null) {
            Integer tenure = productMaxTenureMonths != null ? productMaxTenureMonths : tenureMonths;
            if (maturityDate == null && tenure != null && tenure > 0) {
                maturityDate = createdAt.plusMonths(tenure);
            }
            if (nextInterestAccrualAt == null) {
                nextInterestAccrualAt = createdAt.plusYears(1);
            }
            if (nextPayoutAt == null && maturityDate != null) {
                nextPayoutAt = maturityDate;
            }
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = TimeProvider.currentDateTime();
    }

    public enum AccountStatus {
        ACTIVE,
        CLOSED,
        SUSPENDED,
        MATURED
    }
}
