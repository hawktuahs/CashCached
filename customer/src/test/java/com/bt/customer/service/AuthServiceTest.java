package com.bt.customer.service;

import com.bt.customer.dto.AuthResponse;
import com.bt.customer.dto.LoginRequest;
import com.bt.customer.dto.RegisterRequest;
import com.bt.customer.entity.User;
import com.bt.customer.exception.InvalidCredentialsException;
import com.bt.customer.exception.UserAlreadyExistsException;
import com.bt.customer.repository.UserRepository;
import com.bt.customer.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

        @Mock
        private UserRepository userRepository;

        @Mock
        private PasswordEncoder passwordEncoder;

        @Mock
        private JwtTokenProvider tokenProvider;

        @Mock
        private AuthenticationManager authenticationManager;

        @Mock
        private CustomerService customerService;

        @Mock
        private RedisOtpService redisOtpService;

        @Mock
        private RedisSessionService redisSessionService;

        @InjectMocks
        private AuthService authService;

        private RegisterRequest registerRequest;
        private LoginRequest loginRequest;
        private User user;

        @BeforeEach
        void setUp() {
                registerRequest = RegisterRequest.builder()
                                .email("test@example.com")
                                .password("password123")
                                .fullName("Test User")
                                .phoneNumber("+1234567890")
                                .role(User.Role.CUSTOMER)
                                .build();

                loginRequest = LoginRequest.builder()
                                .email("test@example.com")
                                .password("password123")
                                .build();

                user = User.builder()
                                .id(1L)
                                .password("encoded-password")
                                .fullName("Test User")
                                .email("test@example.com")
                                .phoneNumber("+1234567890")
                                .role(User.Role.CUSTOMER)
                                .active(true)
                                .build();
        }

        @Test
        @DisplayName("Should register user successfully with valid data")
        void shouldRegisterUserSuccessfully() {
                when(userRepository.existsByEmail(anyString())).thenReturn(false);
                when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
                when(userRepository.save(any(User.class))).thenReturn(user);
                when(tokenProvider.generateTokenForUser(anyString(), anyString())).thenReturn("jwt-token");

                AuthResponse response = authService.register(registerRequest);

                assertNotNull(response);
                assertEquals("jwt-token", response.getToken());
                assertEquals("test@example.com", response.getEmail());
                assertEquals("CUSTOMER", response.getRole());
                assertEquals("User registered successfully", response.getMessage());

                verify(userRepository, times(1)).existsByEmail("test@example.com");
                verify(userRepository, times(1)).save(any(User.class));
                verify(tokenProvider, times(1)).generateTokenForUser("test@example.com", "CUSTOMER");
        }

        @Test
        @DisplayName("Should throw exception when email already exists")
        void shouldThrowExceptionWhenEmailExists() {
                when(userRepository.existsByEmail(anyString())).thenReturn(true);

                UserAlreadyExistsException exception = assertThrows(
                                UserAlreadyExistsException.class,
                                () -> authService.register(registerRequest));

                assertTrue(exception.getMessage().contains("Email already registered"));
                verify(userRepository, times(1)).existsByEmail("test@example.com");
                verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should register user with default CUSTOMER role when role is null")
        void shouldRegisterUserWithDefaultRole() {
                registerRequest.setRole(null);
                when(userRepository.existsByEmail(anyString())).thenReturn(false);
                when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
                when(userRepository.save(any(User.class))).thenReturn(user);
                when(tokenProvider.generateTokenForUser(anyString(), anyString())).thenReturn("jwt-token");

                AuthResponse response = authService.register(registerRequest);

                assertNotNull(response);
                assertEquals("CUSTOMER", response.getRole());
        }

        @Test
        @DisplayName("Should login user successfully with valid credentials")
        void shouldLoginUserSuccessfully() {
                when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
                when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

                Authentication authentication = mock(Authentication.class);
                when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                                .thenReturn(authentication);

                SecurityContext securityContext = mock(SecurityContext.class);
                try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
                        mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);

                        when(customerService.isTwoFactorEnabledForCurrentUser()).thenReturn(false);
                        when(redisSessionService.createSession(any(User.class))).thenReturn("session-id");

                        AuthResponse response = authService.login(loginRequest);

                        assertNotNull(response);
                        assertEquals("test@example.com", response.getEmail());
                        assertEquals("CUSTOMER", response.getRole());
                        assertEquals("Authentication successful", response.getMessage());

                        verify(userRepository, times(1)).findByEmail("test@example.com");
                        verify(passwordEncoder, times(1)).matches("password123", "encoded-password");
                }
        }

        @Test
        @DisplayName("Should throw exception for invalid email")
        void shouldThrowExceptionForInvalidEmail() {
                when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

                InvalidCredentialsException exception = assertThrows(
                                InvalidCredentialsException.class,
                                () -> authService.login(loginRequest));

                assertTrue(exception.getMessage().contains("Invalid email"));
                verify(userRepository, times(1)).findByEmail("test@example.com");
        }

        @Test
        @DisplayName("Should throw exception for invalid password")
        void shouldThrowExceptionForInvalidPassword() {
                when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
                when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

                InvalidCredentialsException exception = assertThrows(
                                InvalidCredentialsException.class,
                                () -> authService.login(loginRequest));

                assertTrue(exception.getMessage().contains("Invalid password"));
        }
}
