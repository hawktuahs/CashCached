package com.bt.product.repository;

import com.bt.product.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import java.time.LocalDate;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ProductRepositoryTest {

        @Autowired
        private ProductRepository productRepository;

        @Test
        void findByProductCode_ReturnsProduct() {
                Product product = Product.builder()
                                .productCode("TEST001")
                                .productName("Test Product")
                                .productType(ProductType.SAVINGS_ACCOUNT)
                                .currency(Currency.USD)
                                .status(ProductStatus.ACTIVE)
                                .effectiveDate(LocalDate.now())
                                .build();

                productRepository.save(product);

                Product found = productRepository.findByProductCode("TEST001").orElse(null);

                assertThat(found).isNotNull();
                assertThat(found.getProductCode()).isEqualTo("TEST001");
        }

        @Test
        void existsByProductCode_ReturnsTrue() {
                Product product = Product.builder()
                                .productCode("TEST002")
                                .productName("Test Product 2")
                                .productType(ProductType.FIXED_DEPOSIT)
                                .currency(Currency.INR)
                                .status(ProductStatus.ACTIVE)
                                .effectiveDate(LocalDate.now())
                                .build();

                productRepository.save(product);

                boolean exists = productRepository.existsByProductCode("TEST002");

                assertThat(exists).isTrue();
        }

        @Test
        void findByProductType_ReturnsProducts() {
                Product product1 = Product.builder()
                                .productCode("SAV001")
                                .productName("Savings 1")
                                .productType(ProductType.SAVINGS_ACCOUNT)
                                .currency(Currency.USD)
                                .status(ProductStatus.ACTIVE)
                                .effectiveDate(LocalDate.now())
                                .build();

                Product product2 = Product.builder()
                                .productCode("SAV002")
                                .productName("Savings 2")
                                .productType(ProductType.SAVINGS_ACCOUNT)
                                .currency(Currency.USD)
                                .status(ProductStatus.ACTIVE)
                                .effectiveDate(LocalDate.now())
                                .build();

                productRepository.save(product1);
                productRepository.save(product2);

                List<Product> products = productRepository.findByProductType(ProductType.SAVINGS_ACCOUNT);

                assertThat(products).hasSize(2);
        }

        @Test
        void searchProducts_WithFilters_ReturnsFilteredProducts() {
                Product product = Product.builder()
                                .productCode("LOAN001")
                                .productName("Personal Loan")
                                .productType(ProductType.PERSONAL_LOAN)
                                .currency(Currency.USD)
                                .status(ProductStatus.ACTIVE)
                                .effectiveDate(LocalDate.now().minusDays(30))
                                .expiryDate(LocalDate.now().plusDays(365))
                                .build();

                productRepository.save(product);

                List<Product> results = productRepository.searchProducts(
                                ProductType.PERSONAL_LOAN,
                                Currency.USD,
                                ProductStatus.ACTIVE,
                                LocalDate.now(),
                                LocalDate.now().plusDays(300));

                assertThat(results).isNotEmpty();
        }
}
