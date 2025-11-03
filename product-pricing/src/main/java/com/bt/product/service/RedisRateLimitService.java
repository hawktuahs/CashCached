package com.bt.product.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisRateLimitService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String RATE_LIMIT_PREFIX = "rate:limit:";

    public boolean isAllowed(String key, int maxRequests, Duration window) {
        String redisKey = RATE_LIMIT_PREFIX + key;
        Long currentCount = redisTemplate.opsForValue().increment(redisKey);

        if (currentCount == null) {
            return false;
        }

        if (currentCount == 1) {
            redisTemplate.expire(redisKey, window.getSeconds(), TimeUnit.SECONDS);
        }

        boolean allowed = currentCount <= maxRequests;

        if (!allowed) {
            log.warn("Rate limit exceeded for key: {}, count: {}", key, currentCount);
        }

        return allowed;
    }

    public Long getRemainingRequests(String key, int maxRequests) {
        String redisKey = RATE_LIMIT_PREFIX + key;
        Object count = redisTemplate.opsForValue().get(redisKey);

        if (count == null) {
            return (long) maxRequests;
        }

        long currentCount = Long.parseLong(count.toString());
        return Math.max(0, maxRequests - currentCount);
    }

    public void resetLimit(String key) {
        String redisKey = RATE_LIMIT_PREFIX + key;
        redisTemplate.delete(redisKey);
        log.debug("Reset rate limit for key: {}", key);
    }
}
