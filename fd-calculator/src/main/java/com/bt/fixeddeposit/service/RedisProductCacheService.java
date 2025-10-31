package com.bt.fixeddeposit.service;

import com.bt.fixeddeposit.dto.external.ProductResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisProductCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String PRODUCT_PREFIX = "product:";
    private static final long CACHE_TTL_HOURS = 6;

    @Cacheable(value = "products", key = "#productCode")
    public ProductResponse getCachedProduct(String productCode) {
        String key = PRODUCT_PREFIX + productCode;
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            log.debug("Cache hit for product: {}", productCode);
            return (ProductResponse) cached;
        }
        return null;
    }

    public void cacheProduct(String productCode, ProductResponse product) {
        String key = PRODUCT_PREFIX + productCode;
        redisTemplate.opsForValue().set(key, product, CACHE_TTL_HOURS, TimeUnit.HOURS);
        log.debug("Cached product: {}", productCode);
    }

    public void invalidateProduct(String productCode) {
        String key = PRODUCT_PREFIX + productCode;
        redisTemplate.delete(key);
        log.info("Invalidated cached product: {}", productCode);
    }

    public void invalidateAllProducts() {
        var keys = redisTemplate.keys(PRODUCT_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("Invalidated {} cached products", keys.size());
        }
    }
}
