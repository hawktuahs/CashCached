package com.bt.accounts.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisDistributedLockService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String LOCK_PREFIX = "lock:";

    public String acquireLock(String lockKey, long timeoutSeconds) {
        String fullKey = LOCK_PREFIX + lockKey;
        String lockValue = UUID.randomUUID().toString();

        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(fullKey, lockValue, timeoutSeconds, TimeUnit.SECONDS);

        if (Boolean.TRUE.equals(acquired)) {
            log.debug("Lock acquired: {}", lockKey);
            return lockValue;
        }

        log.debug("Failed to acquire lock: {}", lockKey);
        return null;
    }

    public boolean releaseLock(String lockKey, String lockValue) {
        String fullKey = LOCK_PREFIX + lockKey;
        Object currentValue = redisTemplate.opsForValue().get(fullKey);

        if (lockValue.equals(currentValue)) {
            redisTemplate.delete(fullKey);
            log.debug("Lock released: {}", lockKey);
            return true;
        }

        log.warn("Failed to release lock - value mismatch: {}", lockKey);
        return false;
    }

    public boolean extendLock(String lockKey, String lockValue, long additionalSeconds) {
        String fullKey = LOCK_PREFIX + lockKey;
        Object currentValue = redisTemplate.opsForValue().get(fullKey);

        if (lockValue.equals(currentValue)) {
            return Boolean.TRUE.equals(redisTemplate.expire(fullKey, additionalSeconds, TimeUnit.SECONDS));
        }

        return false;
    }
}
