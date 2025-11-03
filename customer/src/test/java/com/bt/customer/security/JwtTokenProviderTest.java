package com.bt.customer.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtTokenProvider Tests")
class JwtTokenProviderTest {

    @InjectMocks
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret",
                "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpiration", 86400000L);
    }

    @Test
    @DisplayName("Should generate valid JWT token from authentication")
    void shouldGenerateTokenFromAuthentication() {
        when(authentication.getName()).thenReturn("testuser");

        String token = jwtTokenProvider.generateToken(authentication);

        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.split("\\.").length == 3);
    }

    @Test
    @DisplayName("Should generate valid JWT token for user with role")
    void shouldGenerateTokenForUserWithRole() {
        String token = jwtTokenProvider.generateTokenForUser("testuser", "CUSTOMER");

        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.split("\\.").length == 3);
    }

    @Test
    @DisplayName("Should extract username from valid token")
    void shouldExtractUsernameFromToken() {
        String token = jwtTokenProvider.generateTokenForUser("testuser", "CUSTOMER");

        String username = jwtTokenProvider.getUsernameFromToken(token);

        assertEquals("testuser", username);
    }

    @Test
    @DisplayName("Should validate valid token successfully")
    void shouldValidateValidToken() {
        String token = jwtTokenProvider.generateTokenForUser("testuser", "CUSTOMER");

        boolean isValid = jwtTokenProvider.validateToken(token);

        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should reject invalid token")
    void shouldRejectInvalidToken() {
        String invalidToken = "invalid.jwt.token";

        boolean isValid = jwtTokenProvider.validateToken(invalidToken);

        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should reject malformed token")
    void shouldRejectMalformedToken() {
        String malformedToken = "malformed-token-without-dots";

        boolean isValid = jwtTokenProvider.validateToken(malformedToken);

        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should reject empty token")
    void shouldRejectEmptyToken() {
        boolean isValid = jwtTokenProvider.validateToken("");

        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should reject null token")
    void shouldRejectNullToken() {
        boolean isValid = jwtTokenProvider.validateToken(null);

        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should generate different tokens for different users")
    void shouldGenerateDifferentTokensForDifferentUsers() {
        String token1 = jwtTokenProvider.generateTokenForUser("user1", "CUSTOMER");
        String token2 = jwtTokenProvider.generateTokenForUser("user2", "ADMIN");

        assertNotEquals(token1, token2);

        String username1 = jwtTokenProvider.getUsernameFromToken(token1);
        String username2 = jwtTokenProvider.getUsernameFromToken(token2);

        assertEquals("user1", username1);
        assertEquals("user2", username2);
    }

    @Test
    @DisplayName("Should include role claim in token")
    void shouldIncludeRoleClaimInToken() {
        String token = jwtTokenProvider.generateTokenForUser("testuser", "ADMIN");

        assertNotNull(token);
        String username = jwtTokenProvider.getUsernameFromToken(token);
        assertEquals("testuser", username);
    }
}
