package com.bt.accounts.controller;

import com.bt.accounts.dto.*;
import com.bt.accounts.security.JwtAuthenticationFilter;
import com.bt.accounts.security.JwtTokenProvider;
import com.bt.accounts.service.AccountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AccountController.class)
class AccountControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private AccountService accountService;

        @MockitoBean
        private JwtTokenProvider jwtTokenProvider;

        @MockitoBean
        private JwtAuthenticationFilter jwtAuthenticationFilter;

        private AccountCreationRequest validRequest;
        private AccountResponse accountResponse;

        @BeforeEach
        void setUp() {
                validRequest = AccountCreationRequest.builder()
                                .customerId("CUST001")
                                .productCode("FD-PREMIUM")
                                .principalAmount(new BigDecimal("100000.00"))
                                .interestRate(new BigDecimal("6.75"))
                                .tenureMonths(24)
                                .branchCode("BR001")
                                .build();

                accountResponse = AccountResponse.builder()
                                .id(1L)
                                .accountNo("FD-BR001-20251023-10000001")
                                .customerId("CUST001")
                                .productCode("FD-PREMIUM")
                                .principalAmount(new BigDecimal("100000.00"))
                                .status("ACTIVE")
                                .build();
        }

        @Test
        @WithMockUser(roles = "BANKOFFICER")
        void createAccount_WithValidRequest_ShouldReturn201() throws Exception {
                when(accountService.createAccount(any(AccountCreationRequest.class), any()))
                                .thenReturn(accountResponse);

                mockMvc.perform(post("/api/accounts/create")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer test-token")
                                .content(objectMapper.writeValueAsString(validRequest)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.accountNo").value("FD-BR001-20251023-10000001"))
                                .andExpect(jsonPath("$.data.customerId").value("CUST001"));
        }

        @Test
        @WithMockUser(roles = "BANKOFFICER")
        void createAccount_WithInvalidPrincipal_ShouldReturn400() throws Exception {
                validRequest.setPrincipalAmount(new BigDecimal("500"));

                mockMvc.perform(post("/api/accounts/create")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer test-token")
                                .content(objectMapper.writeValueAsString(validRequest)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser
        void getAccount_WithValidAccountNo_ShouldReturn200() throws Exception {
                when(accountService.getAccount("FD-BR001-20251023-10000001"))
                                .thenReturn(accountResponse);

                mockMvc.perform(get("/api/accounts/FD-BR001-20251023-10000001")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.accountNo").value("FD-BR001-20251023-10000001"));
        }

        @Test
        @WithMockUser
        void getCustomerAccounts_WithValidCustomerId_ShouldReturn200() throws Exception {
                List<AccountResponse> accounts = Arrays.asList(accountResponse);
                when(accountService.getCustomerAccounts("CUST001")).thenReturn(accounts);

                mockMvc.perform(get("/api/accounts/customer/CUST001")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data").isArray())
                                .andExpect(jsonPath("$.data[0].customerId").value("CUST001"));
        }

        @Test
        @WithMockUser(roles = "BANKOFFICER")
        void closeAccount_WithValidRequest_ShouldReturn200() throws Exception {
                AccountClosureRequest closureRequest = AccountClosureRequest.builder()
                                .closureReason("Customer requested premature closure")
                                .build();

                accountResponse.setStatus("CLOSED");
                when(accountService.closeAccount(eq("FD-BR001-20251023-10000001"), any()))
                                .thenReturn(accountResponse);

                mockMvc.perform(put("/api/accounts/FD-BR001-20251023-10000001/close")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(closureRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.status").value("CLOSED"));
        }

        @Test
        @WithMockUser(roles = "USER")
        void createAccount_WithoutBankOfficerRole_ShouldReturn403() throws Exception {
                mockMvc.perform(post("/api/accounts/create")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer test-token")
                                .content(objectMapper.writeValueAsString(validRequest)))
                                .andExpect(status().isForbidden());
        }
}
