package com.bt.customer.service;

import com.bt.customer.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
public class RedisSessionServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private RedisSessionService redisSessionService;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"username\":\"testuser\"}");
        ReflectionTestUtils.setField(redisSessionService, "sessionTimeoutSeconds", 3600L);
        ReflectionTestUtils.setField(redisSessionService, "idleTimeoutSeconds", 900L);
    }

    @Test
    public void testCreateSession() {
        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .role(User.Role.CUSTOMER)
                .build();

        when(valueOperations.get("user_sessions:test@example.com")).thenReturn(null);

        String sessionId = redisSessionService.createSession(user);

        assertNotNull(sessionId);
        verify(valueOperations, atLeast(1)).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    public void testEnforceOneLoginPerUser() {
        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .role(User.Role.CUSTOMER)
                .build();

        when(valueOperations.get("user_sessions:test@example.com")).thenReturn("existing-session-id");

        String newSessionId = redisSessionService.createSession(user);

        assertNotNull(newSessionId);
    }

    @Test
    public void testUpdateIdleTimeout() {
        String sessionId = "test-session-id";

        redisSessionService.updateIdleTimeout(sessionId);

        verify(valueOperations).set(
                eq("session_idle:" + sessionId),
                anyString(),
                eq(900L),
                eq(TimeUnit.SECONDS));
    }

    @Test
    public void testIsSessionValid() {
        String sessionId = "valid-session-id";

        when(valueOperations.get("session:" + sessionId))
                .thenReturn("{\"username\":\"testuser\"}");
        when(valueOperations.get("session_idle:" + sessionId))
                .thenReturn(String.valueOf(System.currentTimeMillis() / 1000));

        assertTrue(redisSessionService.isSessionValid(sessionId));
    }

    @Test
    public void testIsSessionInvalid() {
        String sessionId = "invalid-session-id";

        when(valueOperations.get("session:" + sessionId)).thenReturn(null);

        assertFalse(redisSessionService.isSessionValid(sessionId));
    }
}
