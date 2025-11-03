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
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
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

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User user;

    @BeforeEach
    void setUp() {
        registerRequest = RegisterRequest.builder()
                .username("testuser")
                .password("password123")
                .fullName("Test User")
                .email("test@example.com")
                .phoneNumber("+1234567890")
                .role(User.Role.CUSTOMER)
                .build();

        loginRequest = LoginRequest.builder()
                .username("testuser")
                .password("password123")
                .build();

        user = User.builder()
                .id(1L)
                .username("testuser")
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
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(tokenProvider.generateTokenForUser(anyString(), anyString())).thenReturn("jwt-token");

        AuthResponse response = authService.register(registerRequest);

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals("testuser", response.getUsername());
        assertEquals("CUSTOMER", response.getRole());
        assertEquals("User registered successfully", response.getMessage());

        verify(userRepository, times(1)).existsByUsername("testuser");
        verify(userRepository, times(1)).existsByEmail("test@example.com");
        verify(userRepository, times(1)).save(any(User.class));
        verify(tokenProvider, times(1)).generateTokenForUser("testuser", "CUSTOMER");
    }

    @Test
    @DisplayName("Should throw exception when username already exists")
    void shouldThrowExceptionWhenUsernameExists() {
        when(userRepository.existsByUsername(anyString())).thenReturn(true);

        UserAlreadyExistsException exception = assertThrows(
                UserAlreadyExistsException.class,
                () -> authService.register(registerRequest));

        assertTrue(exception.getMessage().contains("Username already exists"));
        verify(userRepository, times(1)).existsByUsername("testuser");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when email already exists")
    void shouldThrowExceptionWhenEmailExists() {
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
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
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
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
        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user));
        when(tokenProvider.generateTokenForUser(anyString(), anyString())).thenReturn("jwt-token");

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals("testuser", response.getUsername());
        assertEquals("CUSTOMER", response.getRole());
        assertEquals("Authentication successful", response.getMessage());

        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository, times(1)).findByUsername("testuser");
        verify(tokenProvider, times(1)).generateTokenForUser("testuser", "CUSTOMER");
    }

    @Test
    @DisplayName("Should throw exception for invalid credentials")
    void shouldThrowExceptionForInvalidCredentials() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(loginRequest));

        assertTrue(exception.getMessage().contains("Invalid username or password"));
        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("Should throw exception when user not found after authentication")
    void shouldThrowExceptionWhenUserNotFoundAfterAuth() {
        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());

        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(loginRequest));

        assertTrue(exception.getMessage().contains("Invalid username or password"));
    }
}
