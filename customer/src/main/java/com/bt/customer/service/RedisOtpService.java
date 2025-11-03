package com.bt.customer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@Slf4j
public class RedisOtpService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private static final String OTP_PREFIX = "otp:";
    private static final Duration OTP_VALIDITY = Duration.ofMinutes(5);

    public RedisOtpService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public void storeOtp(String username, String code) {
        String key = OTP_PREFIX + username;
        OtpEntry entry = new OtpEntry(code, Instant.now().plus(OTP_VALIDITY));
        try {
            String json = objectMapper.writeValueAsString(entry);
            stringRedisTemplate.opsForValue().set(key, json, OTP_VALIDITY);
            log.debug("Stored OTP for user: {}", username);
        } catch (Exception e) {
            log.error("Failed to store OTP for user: {}", username, e);
        }
    }

    public boolean validateOtp(String username, String code) {
        String key = OTP_PREFIX + username;
        String json = stringRedisTemplate.opsForValue().get(key);

        if (json == null) {
            log.debug("No OTP found for user: {}", username);
            return false;
        }

        try {
            OtpEntry entry = objectMapper.readValue(json, OtpEntry.class);
            boolean valid = entry.code.equals(code) && entry.expiresAt.isAfter(Instant.now());

            if (valid) {
                stringRedisTemplate.delete(key);
                log.info("OTP validated and removed for user: {}", username);
            } else {
                log.warn("Invalid or expired OTP for user: {}", username);
            }

            return valid;
        } catch (Exception e) {
            log.error("Failed to deserialize OTP for user: {}", username, e);
            return false;
        }
    }

    public void removeOtp(String username) {
        String key = OTP_PREFIX + username;
        stringRedisTemplate.delete(key);
        log.debug("Removed OTP for user: {}", username);
    }

    public static class OtpEntry {
        public String code;
        public Instant expiresAt;

        public OtpEntry() {
        }

        public OtpEntry(String code, Instant expiresAt) {
            this.code = code;
            this.expiresAt = expiresAt;
        }
    }
}
