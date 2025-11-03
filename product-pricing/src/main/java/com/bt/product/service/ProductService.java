package com.bt.product.service;

import com.bt.product.dto.*;
import com.bt.product.entity.Product;
import com.bt.product.entity.ProductStatus;
import com.bt.product.exception.DuplicateProductException;
import com.bt.product.exception.InvalidProductDataException;
import com.bt.product.exception.ProductNotFoundException;
import com.bt.product.repository.ProductRepository;
import com.bt.product.repository.PricingRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final PricingRuleRepository pricingRuleRepository;

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductResponse createProduct(ProductRequest request) {
        validateProductRequest(request);

        if (productRepository.existsByProductCode(request.getProductCode())) {
            throw new DuplicateProductException("Product with code " + request.getProductCode() + " already exists");
        }

        Product product = Product.builder()
                .productCode(request.getProductCode())
                .productName(request.getProductName())
                .productType(request.getProductType())
                .description(request.getDescription())
                .minInterestRate(request.getMinInterestRate())
                .maxInterestRate(request.getMaxInterestRate())
                .minTermMonths(request.getMinTermMonths())
                .maxTermMonths(request.getMaxTermMonths())
                .minAmount(request.getMinAmount())
                .maxAmount(request.getMaxAmount())
                .currency(request.getCurrency())
                .status(request.getStatus() != null ? request.getStatus() : ProductStatus.ACTIVE)
                .effectiveDate(request.getEffectiveDate())
                .expiryDate(request.getExpiryDate())
                .regulatoryCode(request.getRegulatoryCode())
                .requiresApproval(request.getRequiresApproval())
                .compoundingFrequency(request.getCompoundingFrequency())
                .build();

        Product savedProduct = productRepository.save(product);
        return mapToProductResponse(savedProduct);
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductResponse updateProduct(String productCode, ProductRequest request) {
        Product product = productRepository.findByProductCode(productCode)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with code: " + productCode));

        validateProductRequest(request);

        product.setProductName(request.getProductName());
        product.setProductType(request.getProductType());
        product.setDescription(request.getDescription());
        product.setMinInterestRate(request.getMinInterestRate());
        product.setMaxInterestRate(request.getMaxInterestRate());
        product.setMinTermMonths(request.getMinTermMonths());
        product.setMaxTermMonths(request.getMaxTermMonths());
        product.setMinAmount(request.getMinAmount());
        product.setMaxAmount(request.getMaxAmount());
        product.setCurrency(request.getCurrency());
        product.setStatus(request.getStatus());
        product.setEffectiveDate(request.getEffectiveDate());
        product.setExpiryDate(request.getExpiryDate());
        product.setRegulatoryCode(request.getRegulatoryCode());
        product.setRequiresApproval(request.getRequiresApproval());
        product.setCompoundingFrequency(request.getCompoundingFrequency());

        Product updatedProduct = productRepository.save(product);
        return mapToProductResponse(updatedProduct);
    }

    @Cacheable(value = "products", key = "#productCode")
    public ProductResponse getProductByCode(String productCode) {
        Product product = productRepository.findByProductCode(productCode)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with code: " + productCode));
        return mapToProductResponse(product);
    }

    @Cacheable(value = "products")
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::mapToProductResponse)
                .collect(Collectors.toList());
    }

    public List<ProductResponse> searchProducts(ProductSearchRequest searchRequest) {
        List<Product> products = productRepository.searchProducts(
                searchRequest.getProductType(),
                searchRequest.getCurrency(),
                searchRequest.getStatus(),
                searchRequest.getEffectiveDate(),
                searchRequest.getExpiryDate());

        return products.stream()
                .map(this::mapToProductResponse)
                .collect(Collectors.toList());
    }

    public ProductStatusResponse getProductStatus(String productCode) {
        Product product = productRepository.findByProductCode(productCode)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with code: " + productCode));

        boolean isValid = validateProductStatus(product);
        boolean isActive = product.getStatus() == ProductStatus.ACTIVE;

        List<PricingRuleResponse> pricingRules = pricingRuleRepository.findActiveRulesByProductCode(productCode)
                .stream()
                .map(this::mapToPricingRuleResponse)
                .collect(Collectors.toList());

        return ProductStatusResponse.builder()
                .productCode(product.getProductCode())
                .productName(product.getProductName())
                .status(product.getStatus())
                .isValid(isValid)
                .isActive(isActive)
                .effectiveDate(product.getEffectiveDate())
                .expiryDate(product.getExpiryDate())
                .currentInterestRate(product.getMinInterestRate())
                .applicablePricingRules(pricingRules)
                .validationMessage(
                        isValid ? "Product is valid and operational" : "Product has expired or is not yet effective")
                .build();
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public void deleteProduct(String productCode) {
        Product product = productRepository.findByProductCode(productCode)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with code: " + productCode));
        productRepository.delete(product);
    }

    private void validateProductRequest(ProductRequest request) {
        if (request.getMinInterestRate() != null && request.getMaxInterestRate() != null) {
            if (request.getMinInterestRate().compareTo(request.getMaxInterestRate()) > 0) {
                throw new InvalidProductDataException(
                        "Minimum interest rate cannot be greater than maximum interest rate");
            }
        }

        if (request.getMinTermMonths() != null && request.getMaxTermMonths() != null) {
            if (request.getMinTermMonths() > request.getMaxTermMonths()) {
                throw new InvalidProductDataException("Minimum term cannot be greater than maximum term");
            }
        }

        if (request.getMinAmount() != null && request.getMaxAmount() != null) {
            if (request.getMinAmount().compareTo(request.getMaxAmount()) > 0) {
                throw new InvalidProductDataException("Minimum amount cannot be greater than maximum amount");
            }
        }

        if (request.getExpiryDate() != null && request.getEffectiveDate() != null) {
            if (request.getExpiryDate().isBefore(request.getEffectiveDate())) {
                throw new InvalidProductDataException("Expiry date cannot be before effective date");
            }
        }
    }

    private boolean validateProductStatus(Product product) {
        LocalDate today = LocalDate.now();
        return product.getEffectiveDate().isBefore(today.plusDays(1)) &&
                (product.getExpiryDate() == null || product.getExpiryDate().isAfter(today.minusDays(1)));
    }

    private ProductResponse mapToProductResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .productCode(product.getProductCode())
                .productName(product.getProductName())
                .productType(product.getProductType())
                .description(product.getDescription())
                .minInterestRate(product.getMinInterestRate())
                .maxInterestRate(product.getMaxInterestRate())
                .minTermMonths(product.getMinTermMonths())
                .maxTermMonths(product.getMaxTermMonths())
                .minAmount(product.getMinAmount())
                .maxAmount(product.getMaxAmount())
                .currency(product.getCurrency())
                .status(product.getStatus())
                .effectiveDate(product.getEffectiveDate())
                .expiryDate(product.getExpiryDate())
                .regulatoryCode(product.getRegulatoryCode())
                .requiresApproval(product.getRequiresApproval())
                .compoundingFrequency(product.getCompoundingFrequency())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    private PricingRuleResponse mapToPricingRuleResponse(com.bt.product.entity.PricingRule rule) {
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
