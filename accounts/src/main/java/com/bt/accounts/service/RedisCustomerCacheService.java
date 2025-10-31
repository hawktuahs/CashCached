package com.bt.accounts.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisCustomerCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String CUSTOMER_PREFIX = "customer:";
    private static final long CACHE_TTL_HOURS = 6;

    @Cacheable(value = "customers", key = "#customerId")
    public Object getCachedCustomer(Long customerId) {
        String key = CUSTOMER_PREFIX + customerId;
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            log.debug("Cache hit for customer: {}", customerId);
            return cached;
        }
        return null;
    }

    public void cacheCustomer(Long customerId, Object customer) {
        String key = CUSTOMER_PREFIX + customerId;
        redisTemplate.opsForValue().set(key, customer, CACHE_TTL_HOURS, TimeUnit.HOURS);
        log.debug("Cached customer: {}", customerId);
    }

    public void invalidateCustomer(Long customerId) {
        String key = CUSTOMER_PREFIX + customerId;
        redisTemplate.delete(key);
        log.info("Invalidated cached customer: {}", customerId);
    }

    public void invalidateAllCustomers() {
        var keys = redisTemplate.keys(CUSTOMER_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("Invalidated {} cached customers", keys.size());
        }
    }
}
