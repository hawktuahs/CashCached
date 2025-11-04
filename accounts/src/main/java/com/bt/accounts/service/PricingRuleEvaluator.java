package com.bt.accounts.service;

import com.bt.accounts.dto.PricingRuleDto;
import com.bt.accounts.entity.FdAccount;
import com.bt.accounts.exception.ServiceIntegrationException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PricingRuleEvaluator {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final PricingRuleClient pricingRuleClient;

    public EvaluationResult evaluate(FdAccount account, BigDecimal balance, String authToken) {
        try {
            String token = resolveToken(authToken);
            List<PricingRuleDto> rules = fetchRules(account, token);
            if (rules.isEmpty()) {
                return EvaluationResult.noRule(account.getBaseInterestRate());
            }
            PricingRuleDto matched = rules.stream()
                    .filter(rule -> matches(rule, balance))
                    .findFirst()
                    .orElse(null);
            if (matched == null) {
                return EvaluationResult.noRule(account.getBaseInterestRate());
            }
            BigDecimal rate = resolveRate(account.getBaseInterestRate(), matched);
            BigDecimal fee = resolveFee(matched);
            return EvaluationResult.ruleMatched(matched, rate, fee);
        } catch (Exception ex) {
            log.warn("Pricing rule evaluation failed for account {}: {}", account.getAccountNo(), ex.getMessage());
            throw new ServiceIntegrationException("Unable to evaluate pricing rules", ex);
        }
    }

    private List<PricingRuleDto> fetchRules(FdAccount account, String token) {
        return pricingRuleClient.fetchActiveRules(account, token);
    }

    private boolean matches(PricingRuleDto rule, BigDecimal balance) {
        if (rule.getMinThreshold() != null && balance.compareTo(rule.getMinThreshold()) < 0) {
            return false;
        }
        if (rule.getMaxThreshold() != null && balance.compareTo(rule.getMaxThreshold()) > 0) {
            return false;
        }
        return Boolean.TRUE.equals(rule.getIsActive());
    }

    private BigDecimal resolveRate(BigDecimal baseRate, PricingRuleDto rule) {
        if (rule.getInterestRate() != null && rule.getInterestRate().compareTo(BigDecimal.ZERO) > 0) {
            return rule.getInterestRate().setScale(2, RoundingMode.HALF_UP);
        }
        if (rule.getDiscountPercentage() != null && rule.getDiscountPercentage().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal discount = baseRate.multiply(rule.getDiscountPercentage()).divide(ONE_HUNDRED, 4,
                    RoundingMode.HALF_UP);
            BigDecimal adjusted = baseRate.subtract(discount);
            if (adjusted.compareTo(BigDecimal.ZERO) < 0) {
                return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            }
            return adjusted.setScale(2, RoundingMode.HALF_UP);
        }
        return baseRate.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal resolveFee(PricingRuleDto rule) {
        if (rule.getFeeAmount() == null || rule.getFeeAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return rule.getFeeAmount().setScale(0, RoundingMode.CEILING);
    }

    private String resolveToken(String authToken) {
        if (authToken != null && !authToken.isBlank()) {
            return authToken;
        }
        throw new ServiceIntegrationException("Missing authorization token");
    }

    public static final class EvaluationResult {
        private final PricingRuleDto rule;
        private final BigDecimal appliedRate;
        private final BigDecimal penalty;

        private EvaluationResult(PricingRuleDto rule, BigDecimal appliedRate, BigDecimal penalty) {
            this.rule = rule;
            this.appliedRate = appliedRate;
            this.penalty = penalty;
        }

        public PricingRuleDto getRule() {
            return rule;
        }

        public BigDecimal getAppliedRate() {
            return appliedRate;
        }

        public BigDecimal getPenalty() {
            return penalty;
        }

        public boolean hasRule() {
            return rule != null;
        }

        public static EvaluationResult noRule(BigDecimal baseRate) {
            BigDecimal rate = baseRate != null ? baseRate.setScale(2, RoundingMode.HALF_UP) : null;
            return new EvaluationResult(null, rate, null);
        }

        public static EvaluationResult ruleMatched(PricingRuleDto rule, BigDecimal appliedRate, BigDecimal penalty) {
            BigDecimal rate = appliedRate != null ? appliedRate.setScale(2, RoundingMode.HALF_UP) : null;
            return new EvaluationResult(rule, rate, penalty);
        }
    }
}
