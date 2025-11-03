package com.bt.fixeddeposit.service;

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
        clearCacheByPattern("fd:calc:*");
        clearCacheByPattern("product:*");
        clearCacheByPattern("customer:*");
        clearCacheByPattern("auth:token:*");
        log.info("All caches cleared");
    }

    public void clearFdCalculationCache() {
        clearCacheByPattern("fd:calc:*");
        log.info("FD calculation cache cleared");
    }

    public void clearProductCache() {
        clearCacheByPattern("product:*");
        log.info("Product cache cleared");
    }

    public void clearCustomerCache() {
        clearCacheByPattern("customer:*");
        log.info("Customer cache cleared");
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
