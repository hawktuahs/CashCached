package com.bt.customer.controller;

import com.bt.customer.dto.UpdateProfileRequest;
import com.bt.customer.dto.UserProfileResponse;
import com.bt.customer.entity.User;
import com.bt.customer.service.CustomerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CustomerController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("CustomerController Tests")
class CustomerControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private CustomerService customerService;

        private UserProfileResponse profileResponse;
        private UpdateProfileRequest updateRequest;
        private User mockUser;

        @BeforeEach
        void setUp() {
                profileResponse = UserProfileResponse.builder()
                                .id(1L)
                                .fullName("Test User")
                                .email("test@example.com")
                                .phoneNumber("+1234567890")
                                .role("CUSTOMER")
                                .active(true)
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build();

                updateRequest = UpdateProfileRequest.builder()
                                .fullName("Updated Name")
                                .email("updated@example.com")
                                .phoneNumber("+9876543210")
                                .build();

                mockUser = User.builder()
                                .id(1L)
                                .fullName("Test User")
                                .email("test@example.com")
                                .role(User.Role.CUSTOMER)
                                .build();
        }

        @Test
        @WithMockUser(username = "testuser", roles = { "CUSTOMER" })
        @DisplayName("Should get current user profile successfully")
        void shouldGetCurrentUserProfile() throws Exception {
                when(customerService.getCurrentUserProfile()).thenReturn(profileResponse);

                mockMvc.perform(get("/api/customer/profile")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.username").value("testuser"))
                                .andExpect(jsonPath("$.email").value("test@example.com"))
                                .andExpect(jsonPath("$.role").value("CUSTOMER"));

                verify(customerService, times(1)).getCurrentUserProfile();
        }

        @Test
        @WithMockUser(username = "admin", roles = { "ADMIN" })
        @DisplayName("Should get all customers when user is ADMIN")
        void shouldGetAllCustomersForAdmin() throws Exception {
                List<UserProfileResponse> customers = Arrays.asList(
                                profileResponse,
                                UserProfileResponse.builder()
                                                .id(2L)
                                                .fullName("Customer Two")
                                                .email("customer2@example.com")
                                                .role("CUSTOMER")
                                                .build());

                when(customerService.getAllCustomers()).thenReturn(customers);

                mockMvc.perform(get("/api/customer/all")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$[0].username").value("testuser"))
                                .andExpect(jsonPath("$[1].username").value("customer2"));

                verify(customerService, times(1)).getAllCustomers();
        }

        @Test
        @WithMockUser(username = "bankofficer", roles = { "BANKOFFICER" })
        @DisplayName("Should get all customers when user is BANKOFFICER")
        void shouldGetAllCustomersForBankOfficer() throws Exception {
                List<UserProfileResponse> customers = Arrays.asList(profileResponse);
                when(customerService.getAllCustomers()).thenReturn(customers);

                mockMvc.perform(get("/api/customer/all")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray());

                verify(customerService, times(1)).getAllCustomers();
        }

        @Test
        @WithMockUser(username = "testuser", roles = { "CUSTOMER" })
        @DisplayName("Should update profile successfully")
        void shouldUpdateProfileSuccessfully() throws Exception {
                UserProfileResponse updatedResponse = UserProfileResponse.builder()
                                .id(1L)
                                .fullName("Updated Name")
                                .email("updated@example.com")
                                .phoneNumber("+9876543210")
                                .role("CUSTOMER")
                                .build();

                when(customerService.updateProfile(any(UpdateProfileRequest.class))).thenReturn(updatedResponse);

                mockMvc.perform(put("/api/customer/update")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.fullName").value("Updated Name"))
                                .andExpect(jsonPath("$.email").value("updated@example.com"));

                verify(customerService, times(1)).updateProfile(any(UpdateProfileRequest.class));
        }

        @Test
        @WithMockUser(username = "testuser", roles = { "CUSTOMER" })
        @DisplayName("Should return bad request for invalid email in update")
        void shouldReturnBadRequestForInvalidEmailUpdate() throws Exception {
                updateRequest.setEmail("invalid-email");

                mockMvc.perform(put("/api/customer/update")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateRequest)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = "testuser", roles = { "CUSTOMER" })
        @DisplayName("Should get service status successfully")
        void shouldGetServiceStatus() throws Exception {
                when(customerService.getCurrentUserRole()).thenReturn("CUSTOMER");
                when(customerService.getCurrentUser()).thenReturn(mockUser);

                mockMvc.perform(get("/api/customer/status")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("OPERATIONAL"))
                                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                                .andExpect(jsonPath("$.username").value("testuser"));

                verify(customerService, times(1)).getCurrentUserRole();
                verify(customerService, times(1)).getCurrentUser();
        }
}
