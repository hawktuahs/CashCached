package com.bt.product.controller;

import com.bt.product.dto.ProductRequest;
import com.bt.product.dto.ProductResponse;
import com.bt.product.entity.Currency;
import com.bt.product.entity.ProductType;
import com.bt.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    private ProductResponse productResponse;

    @BeforeEach
    void setUp() {
        productResponse = ProductResponse.builder()
                .id(1L)
                .productCode("SAV001")
                .productName("Premium Savings Account")
                .productType(ProductType.SAVINGS_ACCOUNT)
                .currency(Currency.USD)
                .minInterestRate(BigDecimal.valueOf(3.5))
                .maxInterestRate(BigDecimal.valueOf(5.5))
                .build();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createProduct_WithAdminRole_ReturnsCreated() throws Exception {
        when(productService.createProduct(any(ProductRequest.class))).thenReturn(productResponse);

        mockMvc.perform(post("/api/v1/product")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                        "{\"productCode\":\"SAV001\",\"productName\":\"Premium Savings Account\",\"productType\":\"SAVINGS_ACCOUNT\",\"currency\":\"USD\",\"effectiveDate\":\"2025-01-01\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void createProduct_WithCustomerRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/product")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"productCode\":\"SAV001\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getProductByCode_WithAuthentication_ReturnsProduct() throws Exception {
        when(productService.getProductByCode("SAV001")).thenReturn(productResponse);

        mockMvc.perform(get("/api/v1/product/SAV001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.productCode").value("SAV001"));
    }

    @Test
    void getProductByCode_WithoutAuthentication_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/product/SAV001"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteProduct_WithAdminRole_ReturnsSuccess() throws Exception {
        mockMvc.perform(delete("/api/v1/product/SAV001")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
