package com.bt.accounts.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

    public List<GrantedAuthority> getAuthoritiesFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        List<GrantedAuthority> authorities = new ArrayList<>();

        // Try to get role from "role" claim (singular)
        String role = claims.get("role", String.class);
        if (role != null) {
            // Add ROLE_ prefix if not present
            if (!role.startsWith("ROLE_")) {
                role = "ROLE_" + role;
            }
            authorities.add(new SimpleGrantedAuthority(role));
            return authorities;
        }

        // Fallback: try to get roles from "roles" claim (plural)
        List<?> roles = claims.get("roles", List.class);
        if (roles != null) {
            roles.forEach(item -> {
                String resolved = extractAuthority(item);
                if (resolved != null) {
                    authorities.add(new SimpleGrantedAuthority(resolved));
                }
            });
        }

        if (!authorities.isEmpty()) {
            return authorities;
        }

        // Final fallback: try generic authorities collection (e.g. Spring tokens)
        List<?> genericAuthorities = claims.get("authorities", List.class);
        if (genericAuthorities != null) {
            genericAuthorities.forEach(item -> {
                String resolved = extractAuthority(item);
                if (resolved != null) {
                    authorities.add(new SimpleGrantedAuthority(resolved));
                }
            });
        }

        return authorities;
    }

    private String extractAuthority(Object item) {
        if (item == null) {
            return null;
        }
        if (item instanceof String str) {
            return normalizeAuthority(str);
        }
        if (item instanceof java.util.Map<?, ?> map) {
            Object value = map.get("authority");
            if (value instanceof String strVal) {
                return normalizeAuthority(strVal);
            }
        }
        return null;
    }

    private String normalizeAuthority(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.startsWith("ROLE_") ? trimmed : "ROLE_" + trimmed;
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }
}
