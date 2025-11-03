package com.bt.customer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisCacheManager {

    private final RedisTemplate<String, Object> redisTemplate;

    public void clearAllCaches() {
        clearCacheByPattern("userProfile:*");
        clearCacheByPattern("customerById:*");
        clearCacheByPattern("allCustomers:*");
        clearCacheByPattern("auth:token:*");
        log.info("All customer caches cleared");
    }

    public void clearUserProfileCache() {
        clearCacheByPattern("userProfile:*");
        log.info("User profile cache cleared");
    }

    public void clearAuthTokens() {
        clearCacheByPattern("auth:token:*");
        log.info("Auth tokens cleared");
    }

    private void clearCacheByPattern(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.debug("Cleared {} keys matching pattern: {}", keys.size(), pattern);
        }
    }

    public long countKeys(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        return keys != null ? keys.size() : 0;
    }
}
