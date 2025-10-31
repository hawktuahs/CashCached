package com.bt.accounts.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisTokenService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String TOKEN_PREFIX = "auth:token:";
    private static final String BLACKLIST_PREFIX = "auth:blacklist:";
    private static final long TOKEN_VALIDITY_HOURS = 24;

    public void storeToken(String token, String username) {
        String key = TOKEN_PREFIX + token;
        redisTemplate.opsForValue().set(key, username, TOKEN_VALIDITY_HOURS, TimeUnit.HOURS);
        log.debug("Stored token for user: {}", username);
    }

    public String getUsernameFromToken(String token) {
        String key = TOKEN_PREFIX + token;
        Object username = redisTemplate.opsForValue().get(key);
        return username != null ? username.toString() : null;
    }

    public boolean isTokenValid(String token) {
        String key = TOKEN_PREFIX + token;
        Boolean hasKey = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(hasKey) && !isTokenBlacklisted(token);
    }

    public void blacklistToken(String token) {
        String blacklistKey = BLACKLIST_PREFIX + token;
        Long ttl = redisTemplate.getExpire(TOKEN_PREFIX + token, TimeUnit.SECONDS);
        if (ttl != null && ttl > 0) {
            redisTemplate.opsForValue().set(blacklistKey, "true", ttl, TimeUnit.SECONDS);
            log.info("Token blacklisted successfully");
        }
    }

    public boolean isTokenBlacklisted(String token) {
        String blacklistKey = BLACKLIST_PREFIX + token;
        Boolean hasKey = redisTemplate.hasKey(blacklistKey);
        return Boolean.TRUE.equals(hasKey);
    }

    public void removeToken(String token) {
        String key = TOKEN_PREFIX + token;
        redisTemplate.delete(key);
        log.debug("Removed token from Redis");
    }

    public void extendTokenValidity(String token, long hours) {
        String key = TOKEN_PREFIX + token;
        redisTemplate.expire(key, hours, TimeUnit.HOURS);
        log.debug("Extended token validity for {} hours", hours);
    }
}
