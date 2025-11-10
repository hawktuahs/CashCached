package com.bt.accounts.service;

import com.bt.accounts.config.AuthServiceProperties;
import com.bt.accounts.exception.ServiceIntegrationException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@Slf4j
public class ServiceTokenProvider {

    private static final Duration REFRESH_LEEWAY = Duration.ofMinutes(2);

    private final RestTemplate restTemplate;
    private final AuthServiceProperties authProperties;
    private final SecretKey signingKey;
    private final String loginEndpoint;
    private final AtomicReference<TokenCache> cache = new AtomicReference<>();

    public ServiceTokenProvider(RestTemplate restTemplate, AuthServiceProperties authProperties,
            @Value("${jwt.secret}") String jwtSecret) {
        this.restTemplate = restTemplate;
        this.authProperties = authProperties;
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        String baseUrl = StringUtils.trimTrailingCharacter(Objects.toString(authProperties.getUrl(), "").trim(), '/');
        this.loginEndpoint = baseUrl.isEmpty() ? "" : baseUrl + "/api/auth/login";
    }

    public String getBearerToken() {
        TokenCache current = cache.get();
        if (current != null && current.isValid()) {
            return current.token();
        }
        synchronized (cache) {
            current = cache.get();
            if (current == null || !current.isValid()) {
                TokenCache refreshed = authenticate();
                cache.set(refreshed);
                current = refreshed;
            }
        }
        return current.token();
    }

    private TokenCache authenticate() {
        if (!StringUtils.hasText(loginEndpoint)) {
            throw new ServiceIntegrationException("Customer auth service URL is not configured");
        }
        String email = authProperties.getUsername();
        String password = authProperties.getPassword();
        if (!StringUtils.hasText(email) || !StringUtils.hasText(password)) {
            throw new ServiceIntegrationException("Customer service credentials are not configured");
        }

        LoginRequest payload = new LoginRequest(email, password);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<LoginRequest> entity = new HttpEntity<>(payload, headers);
        try {
            ResponseEntity<AuthResponse> response = restTemplate.exchange(
                    loginEndpoint, HttpMethod.POST, entity, AuthResponse.class);
            AuthResponse body = response.getBody();
            if (body == null || !StringUtils.hasText(body.getToken())) {
                throw new ServiceIntegrationException("Customer auth service returned empty token");
            }
            if (Boolean.TRUE.equals(body.getTwoFactorRequired())) {
                throw new ServiceIntegrationException("Service account requires OTP; cannot proceed automatically");
            }
            Instant expiry = extractExpiry(body.getToken());
            return new TokenCache(body.getToken(), expiry);
        } catch (RestClientException ex) {
            log.error("Service authentication failed: {}", ex.getMessage());
            throw new ServiceIntegrationException("Unable to authenticate with customer service", ex);
        }
    }

    private Instant extractExpiry(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Date expiration = claims.getExpiration();
            if (expiration != null) {
                return expiration.toInstant();
            }
        } catch (Exception ex) {
            log.warn("Failed to parse JWT expiry, falling back to default window", ex);
        }
        return Instant.now().plus(Duration.ofHours(12));
    }

    private record TokenCache(String token, Instant expiresAt) {

        boolean isValid() {
            if (!StringUtils.hasText(token) || expiresAt == null) {
                return false;
            }
            Instant refreshThreshold = expiresAt.minus(REFRESH_LEEWAY);
            return Instant.now().isBefore(refreshThreshold);
        }
    }

    private record LoginRequest(String email, String password) {
    }

    private static class AuthResponse {
        private String token;
        private String tokenType;
        private String username;
        private String role;
        private Boolean twoFactorRequired;
        private String message;

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getTokenType() {
            return tokenType;
        }

        public void setTokenType(String tokenType) {
            this.tokenType = tokenType;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public Boolean getTwoFactorRequired() {
            return twoFactorRequired;
        }

        public void setTwoFactorRequired(Boolean twoFactorRequired) {
            this.twoFactorRequired = twoFactorRequired;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
