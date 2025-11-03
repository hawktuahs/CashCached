package com.bt.product.service;

import com.bt.product.dto.PricingRuleRequest;
import com.bt.product.dto.PricingRuleResponse;
import com.bt.product.entity.PricingRule;
import com.bt.product.entity.Product;
import com.bt.product.exception.InvalidProductDataException;
import com.bt.product.exception.PricingRuleNotFoundException;
import com.bt.product.exception.ProductNotFoundException;
import com.bt.product.repository.PricingRuleRepository;
import com.bt.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PricingRuleService {

    private final PricingRuleRepository pricingRuleRepository;
    private final ProductRepository productRepository;

    @Transactional
    public PricingRuleResponse createPricingRule(PricingRuleRequest request) {
        validatePricingRuleRequest(request);

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(
                        () -> new ProductNotFoundException("Product not found with id: " + request.getProductId()));

        PricingRule pricingRule = PricingRule.builder()
                .product(product)
                .ruleName(request.getRuleName())
                .ruleDescription(request.getRuleDescription())
                .minThreshold(request.getMinThreshold())
                .maxThreshold(request.getMaxThreshold())
                .interestRate(request.getInterestRate())
                .feeAmount(request.getFeeAmount())
                .discountPercentage(request.getDiscountPercentage())
                .priorityOrder(request.getPriorityOrder())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        PricingRule savedRule = pricingRuleRepository.save(pricingRule);
        return mapToPricingRuleResponse(savedRule);
    }

    @Transactional
    public PricingRuleResponse updatePricingRule(Long ruleId, PricingRuleRequest request) {
        PricingRule pricingRule = pricingRuleRepository.findById(ruleId)
                .orElseThrow(() -> new PricingRuleNotFoundException("Pricing rule not found with id: " + ruleId));

        validatePricingRuleRequest(request);

        pricingRule.setRuleName(request.getRuleName());
        pricingRule.setRuleDescription(request.getRuleDescription());
        pricingRule.setMinThreshold(request.getMinThreshold());
        pricingRule.setMaxThreshold(request.getMaxThreshold());
        pricingRule.setInterestRate(request.getInterestRate());
        pricingRule.setFeeAmount(request.getFeeAmount());
        pricingRule.setDiscountPercentage(request.getDiscountPercentage());
        pricingRule.setPriorityOrder(request.getPriorityOrder());
        pricingRule.setIsActive(request.getIsActive());

        PricingRule updatedRule = pricingRuleRepository.save(pricingRule);
        return mapToPricingRuleResponse(updatedRule);
    }

    public PricingRuleResponse getPricingRuleById(Long ruleId) {
        PricingRule pricingRule = pricingRuleRepository.findById(ruleId)
                .orElseThrow(() -> new PricingRuleNotFoundException("Pricing rule not found with id: " + ruleId));
        return mapToPricingRuleResponse(pricingRule);
    }

    public List<PricingRuleResponse> getPricingRulesByProductId(Long productId) {
        return pricingRuleRepository.findByProductId(productId).stream()
                .map(this::mapToPricingRuleResponse)
                .collect(Collectors.toList());
    }

    public List<PricingRuleResponse> getActivePricingRulesByProductId(Long productId) {
        return pricingRuleRepository.findActiveRulesByProductIdOrderByPriority(productId).stream()
                .map(this::mapToPricingRuleResponse)
                .collect(Collectors.toList());
    }

    public List<PricingRuleResponse> getApplicableRulesForAmount(Long productId, BigDecimal amount) {
        return pricingRuleRepository.findApplicableRulesByAmount(productId, amount).stream()
                .map(this::mapToPricingRuleResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deletePricingRule(Long ruleId) {
        PricingRule pricingRule = pricingRuleRepository.findById(ruleId)
                .orElseThrow(() -> new PricingRuleNotFoundException("Pricing rule not found with id: " + ruleId));
        pricingRuleRepository.delete(pricingRule);
    }

    private void validatePricingRuleRequest(PricingRuleRequest request) {
        if (request.getMinThreshold() != null && request.getMaxThreshold() != null) {
            if (request.getMinThreshold().compareTo(request.getMaxThreshold()) > 0) {
                throw new InvalidProductDataException("Minimum threshold cannot be greater than maximum threshold");
            }
        }
    }

    private PricingRuleResponse mapToPricingRuleResponse(PricingRule rule) {
        return PricingRuleResponse.builder()
                .id(rule.getId())
                .productId(rule.getProduct().getId())
                .productCode(rule.getProduct().getProductCode())
                .ruleName(rule.getRuleName())
                .ruleDescription(rule.getRuleDescription())
                .minThreshold(rule.getMinThreshold())
                .maxThreshold(rule.getMaxThreshold())
                .interestRate(rule.getInterestRate())
                .feeAmount(rule.getFeeAmount())
                .discountPercentage(rule.getDiscountPercentage())
                .priorityOrder(rule.getPriorityOrder())
                .isActive(rule.getIsActive())
                .createdAt(rule.getCreatedAt())
                .updatedAt(rule.getUpdatedAt())
                .build();
    }
}
