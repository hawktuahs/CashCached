package com.bt.product.service;

import com.bt.product.dto.ProductRequest;
import com.bt.product.dto.ProductResponse;
import com.bt.product.entity.Currency;
import com.bt.product.entity.Product;
import com.bt.product.entity.ProductStatus;
import com.bt.product.entity.ProductType;
import com.bt.product.exception.DuplicateProductException;
import com.bt.product.exception.InvalidProductDataException;
import com.bt.product.exception.ProductNotFoundException;
import com.bt.product.repository.ProductRepository;
import com.bt.product.repository.PricingRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private PricingRuleRepository pricingRuleRepository;

    @InjectMocks
    private ProductService productService;

    private ProductRequest productRequest;
    private Product product;

    @BeforeEach
    void setUp() {
        productRequest = ProductRequest.builder()
                .productCode("SAV001")
                .productName("Premium Savings Account")
                .productType(ProductType.SAVINGS_ACCOUNT)
                .minInterestRate(BigDecimal.valueOf(3.5))
                .maxInterestRate(BigDecimal.valueOf(5.5))
                .minTermMonths(6)
                .maxTermMonths(60)
                .minAmount(BigDecimal.valueOf(1000))
                .maxAmount(BigDecimal.valueOf(1000000))
                .currency(Currency.USD)
                .effectiveDate(LocalDate.now())
                .build();

        product = Product.builder()
                .id(1L)
                .productCode("SAV001")
                .productName("Premium Savings Account")
                .productType(ProductType.SAVINGS_ACCOUNT)
                .minInterestRate(BigDecimal.valueOf(3.5))
                .maxInterestRate(BigDecimal.valueOf(5.5))
                .currency(Currency.USD)
                .status(ProductStatus.ACTIVE)
                .build();
    }

    @Test
    void createProduct_Success() {
        when(productRepository.existsByProductCode("SAV001")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductResponse response = productService.createProduct(productRequest);

        assertNotNull(response);
        assertEquals("SAV001", response.getProductCode());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void createProduct_DuplicateCode_ThrowsException() {
        when(productRepository.existsByProductCode("SAV001")).thenReturn(true);

        assertThrows(DuplicateProductException.class, () -> {
            productService.createProduct(productRequest);
        });

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void createProduct_InvalidInterestRange_ThrowsException() {
        productRequest.setMinInterestRate(BigDecimal.valueOf(10.0));
        productRequest.setMaxInterestRate(BigDecimal.valueOf(5.0));

        assertThrows(InvalidProductDataException.class, () -> {
            productService.createProduct(productRequest);
        });
    }

    @Test
    void getProductByCode_Success() {
        when(productRepository.findByProductCode("SAV001")).thenReturn(Optional.of(product));

        ProductResponse response = productService.getProductByCode("SAV001");

        assertNotNull(response);
        assertEquals("SAV001", response.getProductCode());
    }

    @Test
    void getProductByCode_NotFound_ThrowsException() {
        when(productRepository.findByProductCode("INVALID")).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> {
            productService.getProductByCode("INVALID");
        });
    }

    @Test
    void updateProduct_Success() {
        when(productRepository.findByProductCode("SAV001")).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        productRequest.setProductName("Updated Savings Account");
        ProductResponse response = productService.updateProduct("SAV001", productRequest);

        assertNotNull(response);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void deleteProduct_Success() {
        when(productRepository.findByProductCode("SAV001")).thenReturn(Optional.of(product));
        doNothing().when(productRepository).delete(product);

        productService.deleteProduct("SAV001");

        verify(productRepository).delete(product);
    }
}
