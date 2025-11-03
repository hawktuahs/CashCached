package com.bt.fixeddeposit.service;

import com.bt.fixeddeposit.dto.FdCalculationResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class RedisFdCacheService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private static final String FD_CALCULATION_PREFIX = "fd:calc:";
    private static final String FD_CUSTOMER_HISTORY_PREFIX = "fd:history:";
    private static final long CACHE_TTL_HOURS = 24;

    public RedisFdCacheService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public void cacheCalculation(Long customerId, String productCode, Integer tenureMonths,
            BigDecimal principalAmount, FdCalculationResponse response) {
        String key = buildCalculationKey(customerId, productCode, tenureMonths, principalAmount);
        try {
            String json = objectMapper.writeValueAsString(response);
            stringRedisTemplate.opsForValue().set(key, json, CACHE_TTL_HOURS, TimeUnit.HOURS);
            log.debug("Cached FD calculation: {}", key);
        } catch (Exception e) {
            log.error("Failed to cache FD calculation", e);
        }
    }

    public FdCalculationResponse getCachedCalculation(Long customerId, String productCode,
            Integer tenureMonths, BigDecimal principalAmount) {
        String key = buildCalculationKey(customerId, productCode, tenureMonths, principalAmount);
        String cached = stringRedisTemplate.opsForValue().get(key);
        if (cached != null) {
            log.debug("Cache hit for FD calculation: {}", key);
            try {
                return objectMapper.readValue(cached, FdCalculationResponse.class);
            } catch (Exception e) {
                log.error("Failed to deserialize cached FD calculation", e);
                return null;
            }
        }
        log.debug("Cache miss for FD calculation: {}", key);
        return null;
    }

    public void invalidateCustomerCalculations(Long customerId) {
        String pattern = FD_CUSTOMER_HISTORY_PREFIX + customerId + ":*";
        var keys = stringRedisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
            log.info("Invalidated {} cached calculations for customer: {}", keys.size(), customerId);
        }
    }

    public void invalidateProductCalculations(String productCode) {
        String pattern = FD_CALCULATION_PREFIX + "*:" + productCode + ":*";
        var keys = stringRedisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
            log.info("Invalidated {} cached calculations for product: {}", keys.size(), productCode);
        }
    }

    private String buildCalculationKey(Long customerId, String productCode,
            Integer tenureMonths, BigDecimal principalAmount) {
        return String.format("%s%d:%s:%d:%s",
                FD_CALCULATION_PREFIX, customerId, productCode, tenureMonths,
                principalAmount.toPlainString());
    }

    public void cacheCustomerHistory(Long customerId, Object history) {
        String key = FD_CUSTOMER_HISTORY_PREFIX + customerId;
        try {
            String json = objectMapper.writeValueAsString(history);
            stringRedisTemplate.opsForValue().set(key, json, CACHE_TTL_HOURS, TimeUnit.HOURS);
            log.debug("Cached customer history for: {}", customerId);
        } catch (Exception e) {
            log.error("Failed to cache customer history", e);
        }
    }

    public Object getCachedCustomerHistory(Long customerId) {
        String key = FD_CUSTOMER_HISTORY_PREFIX + customerId;
        String cached = stringRedisTemplate.opsForValue().get(key);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, Object.class);
            } catch (Exception e) {
                log.error("Failed to deserialize customer history", e);
                return null;
            }
        }
        return null;
    }
}
