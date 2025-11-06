package com.bt.customer.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String SESSION_PREFIX = "session:";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String sessionId = extractSessionId(request);
            log.debug("Extracted sessionId from request: {}", sessionId);

            if (StringUtils.hasText(sessionId) && isSessionValid(sessionId)) {
                Map<String, Object> sessionData = getSessionData(sessionId);
                log.debug("Retrieved sessionData: {}", sessionData);

                if (sessionData != null) {
                    String email = (String) sessionData.get("email");
                    String role = (String) sessionData.get("role");

                    log.debug("Session data - email: {}, role: {}", email, role);

                    if (email != null && !email.isEmpty()) {
                        List<GrantedAuthority> authorities = new ArrayList<>();
                        if (role != null) {
                            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                        }

                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                email, null, authorities);
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        log.debug("Authentication set for user: {} with authorities: {}", email, authorities);
                    }
                } else {
                    log.warn("Session data is null for sessionId: {}", sessionId);
                }
            } else {
                log.warn("No valid session found. sessionId: {}", sessionId);
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context", ex);
        }

        filterChain.doFilter(request, response);
    }

    private String extractSessionId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private boolean isSessionValid(String sessionId) {
        try {
            String sessionKey = SESSION_PREFIX + sessionId;
            String idleKey = "session_idle:" + sessionId;

            String sessionJson = redisTemplate.opsForValue().get(sessionKey);
            if (sessionJson == null) {
                log.warn("Session not found in Redis with key: {}", sessionKey);
                return false;
            }

            String idleData = redisTemplate.opsForValue().get(idleKey);
            if (idleData == null) {
                log.warn("Idle timeout not found in Redis with key: {}", idleKey);
                return false;
            }

            log.debug("Session is valid for sessionId: {}", sessionId);
            return true;
        } catch (Exception e) {
            log.error("Error validating session", e);
            return false;
        }
    }

    private Map<String, Object> getSessionData(String sessionId) {
        try {
            String sessionKey = SESSION_PREFIX + sessionId;
            String sessionJson = redisTemplate.opsForValue().get(sessionKey);
            if (sessionJson != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = objectMapper.readValue(sessionJson, Map.class);
                log.debug("Successfully retrieved session data for: {}", sessionId);
                return data;
            }
            log.warn("No session JSON found in Redis for key: {}", sessionKey);
            return null;
        } catch (Exception e) {
            log.error("Error retrieving session data for sessionId: {}", sessionId, e);
            return null;
        }
    }
}
