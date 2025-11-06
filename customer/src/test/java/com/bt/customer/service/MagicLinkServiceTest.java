package com.bt.customer.service;

import com.bt.customer.dto.AuthResponse;
import com.bt.customer.entity.User;
import com.bt.customer.exception.InvalidCredentialsException;
import com.bt.customer.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
public class MagicLinkServiceTest {

        @Mock
        private RedisTemplate<String, String> redisTemplate;

        @Mock
        private UserRepository userRepository;

        @Mock
        private JavaMailSender mailSender;

        @Mock
        private RedisSessionService redisSessionService;

        @Mock
        private ValueOperations<String, String> valueOperations;

        @InjectMocks
        private MagicLinkService magicLinkService;

        @BeforeEach
        public void setUp() {
                MockitoAnnotations.openMocks(this);
                when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        }

        @Test
        public void testSendMagicLink() {
                User user = User.builder()
                                .id(1L)
                                .email("test@example.com")
                                .fullName("Test User")
                                .active(true)
                                .build();

                when(userRepository.findByEmail("test@example.com"))
                                .thenReturn(Optional.of(user));

                magicLinkService.sendMagicLink("test@example.com");

                verify(valueOperations).set(
                                anyString(),
                                eq("test@example.com"),
                                eq(15L),
                                eq(TimeUnit.MINUTES));
        }

        @Test
        public void testSendMagicLinkEmailNotFound() {
                when(userRepository.findByEmail("nonexistent@example.com"))
                                .thenReturn(Optional.empty());

                assertThrows(InvalidCredentialsException.class,
                                () -> magicLinkService.sendMagicLink("nonexistent@example.com"));
        }

        @Test
        public void testVerifyMagicLink() {
                String token = "test-token";
                when(valueOperations.get("magic_link:" + token))
                                .thenReturn("test@example.com");

                String email = magicLinkService.verifyMagicLink(token);

                assertNotNull(email);
                verify(redisTemplate).delete("magic_link:" + token);
        }

        @Test
        public void testVerifyMagicLinkExpired() {
                String token = "expired-token";
                when(valueOperations.get("magic_link:" + token))
                                .thenReturn(null);

                assertThrows(InvalidCredentialsException.class, () -> magicLinkService.verifyMagicLink(token));
        }

        @Test
        public void testAuthenticateWithMagicLink() {
                String token = "valid-token";
                User user = User.builder()
                                .id(1L)
                                .email("test@example.com")
                                .fullName("Test User")
                                .active(true)
                                .role(User.Role.CUSTOMER)
                                .build();

                when(valueOperations.get("magic_link:" + token))
                                .thenReturn("test@example.com");
                when(userRepository.findByEmail("test@example.com"))
                                .thenReturn(Optional.of(user));
                when(redisSessionService.createSession(user))
                                .thenReturn("session-id");

                String sessionId = magicLinkService.authenticateWithMagicLink(token);

                assertNotNull(sessionId);
                verify(redisTemplate).delete("magic_link:" + token);
        }

        @Test
        public void testAuthenticateWithInactiveUser() {
                String token = "valid-token";
                User user = User.builder()
                                .id(1L)
                                .email("test@example.com")
                                .active(false)
                                .build();

                when(valueOperations.get("magic_link:" + token))
                                .thenReturn("test@example.com");
                when(userRepository.findByEmail("test@example.com"))
                                .thenReturn(Optional.of(user));

                assertThrows(InvalidCredentialsException.class,
                                () -> magicLinkService.authenticateWithMagicLink(token));
        }

        @Test
        public void testVerifyAndAuthenticateWithMagicLink() {
                String token = "valid-token";
                User user = User.builder()
                                .id(1L)
                                .email("test@example.com")
                                .fullName("Test User")
                                .active(true)
                                .role(User.Role.CUSTOMER)
                                .build();

                when(valueOperations.get("magic_link:" + token))
                                .thenReturn("test@example.com");
                when(userRepository.findByEmail("test@example.com"))
                                .thenReturn(Optional.of(user));
                when(redisSessionService.createSession(user))
                                .thenReturn("session-id");

                AuthResponse response = magicLinkService.verifyAndAuthenticateWithMagicLink(token);

                assertNotNull(response);
                assertNotNull(response.getToken());
                assertNotNull(response.getEmail());
                verify(redisTemplate).delete("magic_link:" + token);
        }
}
