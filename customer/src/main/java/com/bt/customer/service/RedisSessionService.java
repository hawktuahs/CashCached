package com.bt.customer.service;

import com.bt.customer.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class RedisSessionService {

    private static final String SESSION_PREFIX = "session:";
    private static final String USER_SESSIONS_PREFIX = "user_sessions:";
    private static final String IDLE_TIMEOUT_PREFIX = "session_idle:";

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${app.session.timeout-seconds:3600}")
    private long sessionTimeoutSeconds;

    @Value("${app.session.idle-timeout-seconds:900}")
    private long idleTimeoutSeconds;

    public String createSession(User user) {
        String sessionId = UUID.randomUUID().toString();

        String existingSession = getActiveSessionForUser(user.getEmail());
        if (existingSession != null) {
            invalidateSession(existingSession);
        }

        Map<String, Object> sessionData = new HashMap<>();
        sessionData.put("sessionId", sessionId);
        sessionData.put("email", user.getEmail());
        sessionData.put("userId", user.getId());
        sessionData.put("role", user.getRole().name());
        sessionData.put("createdAt", Instant.now().getEpochSecond());
        sessionData.put("lastActivity", Instant.now().getEpochSecond());

        try {
            String sessionJson = objectMapper.writeValueAsString(sessionData);
            String sessionKey = SESSION_PREFIX + sessionId;
            String userSessionKey = USER_SESSIONS_PREFIX + user.getEmail();

            redisTemplate.opsForValue().set(sessionKey, sessionJson, sessionTimeoutSeconds, TimeUnit.SECONDS);
            redisTemplate.opsForValue().set(userSessionKey, sessionId, sessionTimeoutSeconds, TimeUnit.SECONDS);
            redisTemplate.opsForValue().set(IDLE_TIMEOUT_PREFIX + sessionId,
                    String.valueOf(Instant.now().getEpochSecond()), idleTimeoutSeconds, TimeUnit.SECONDS);

            return sessionId;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create session", e);
        }
    }

    public Optional<Map<String, Object>> getSession(String sessionId) {
        try {
            String sessionKey = SESSION_PREFIX + sessionId;
            String sessionJson = redisTemplate.opsForValue().get(sessionKey);

            if (sessionJson != null) {
                updateIdleTimeout(sessionId);
                return Optional.of(objectMapper.readValue(sessionJson, Map.class));
            }
            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public void updateIdleTimeout(String sessionId) {
        String idleKey = IDLE_TIMEOUT_PREFIX + sessionId;
        redisTemplate.opsForValue().set(idleKey, String.valueOf(Instant.now().getEpochSecond()), idleTimeoutSeconds,
                TimeUnit.SECONDS);
    }

    public void invalidateSession(String sessionId) {
        try {
            String sessionKey = SESSION_PREFIX + sessionId;
            String sessionJson = redisTemplate.opsForValue().get(sessionKey);

            if (sessionJson != null) {
                Map<String, Object> sessionData = objectMapper.readValue(sessionJson, Map.class);
                String email = (String) sessionData.get("email");
                String userSessionKey = USER_SESSIONS_PREFIX + email;

                redisTemplate.delete(sessionKey);
                redisTemplate.delete(userSessionKey);
                redisTemplate.delete(IDLE_TIMEOUT_PREFIX + sessionId);
            }
        } catch (Exception e) {
            log.error("Error invalidating session: {}", sessionId, e);
        }
    }

    public String getActiveSessionForUser(String email) {
        try {
            String userSessionKey = USER_SESSIONS_PREFIX + email;
            return redisTemplate.opsForValue().get(userSessionKey);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isSessionValid(String sessionId) {
        try {
            String sessionKey = SESSION_PREFIX + sessionId;
            String idleKey = IDLE_TIMEOUT_PREFIX + sessionId;

            String sessionJson = redisTemplate.opsForValue().get(sessionKey);
            if (sessionJson == null) {
                log.warn("Session not found in Redis: {}", sessionKey);
                return false;
            }

            String idleData = redisTemplate.opsForValue().get(idleKey);
            if (idleData == null) {
                log.warn("Idle timeout not found in Redis: {}", idleKey);
                invalidateSession(sessionId);
                return false;
            }

            log.debug("Session is valid: {}", sessionId);
            return true;
        } catch (Exception e) {
            log.error("Error validating session: {}", sessionId, e);
            return false;
        }
    }

    public void invalidateAllUserSessions(String email) {
        try {
            String userSessionKey = USER_SESSIONS_PREFIX + email;
            String sessionId = redisTemplate.opsForValue().get(userSessionKey);
            if (sessionId != null) {
                invalidateSession(sessionId);
            }
        } catch (Exception e) {
            log.error("Error invalidating user sessions: {}", email, e);
        }
    }

    public Map<String, Object> getSessionData(String sessionId) {
        try {
            String sessionKey = SESSION_PREFIX + sessionId;
            String sessionJson = redisTemplate.opsForValue().get(sessionKey);
            if (sessionJson != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = objectMapper.readValue(sessionJson, Map.class);
                log.debug("Retrieved session data for: {}", sessionId);
                return data;
            }
            log.warn("No session JSON found for: {}", sessionId);
            return null;
        } catch (Exception e) {
            log.error("Error getting session data for: {}", sessionId, e);
            return null;
        }
    }
}
