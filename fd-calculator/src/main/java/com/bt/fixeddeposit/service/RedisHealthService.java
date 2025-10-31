package com.bt.fixeddeposit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisHealthService {

    private final RedisTemplate<String, Object> redisTemplate;

    public Map<String, Object> getHealthStatus() {
        Map<String, Object> health = new HashMap<>();

        try {
            long start = System.currentTimeMillis();
            redisTemplate.opsForValue().set("health:check", "ping", Duration.ofSeconds(5));
            Object result = redisTemplate.opsForValue().get("health:check");
            long latency = System.currentTimeMillis() - start;

            health.put("status", "UP");
            health.put("latency_ms", latency);
            health.put("connection", "OK");
            health.put("read_write", result != null ? "OK" : "FAILED");

            redisTemplate.delete("health:check");
        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            log.error("Redis health check failed", e);
        }

        return health;
    }

    public Map<String, Object> getCacheStatistics() {
        Map<String, Object> stats = new HashMap<>();

        try {
            long fdCalcKeys = countKeysByPattern("fd:calc:*");
            long productKeys = countKeysByPattern("product:*");
            long tokenKeys = countKeysByPattern("auth:token:*");
            long requestKeys = countKeysByPattern("kafka:request:*");

            stats.put("fd_calculations", fdCalcKeys);
            stats.put("products", productKeys);
            stats.put("auth_tokens", tokenKeys);
            stats.put("kafka_requests", requestKeys);
            stats.put("total_keys", fdCalcKeys + productKeys + tokenKeys + requestKeys);
        } catch (Exception e) {
            stats.put("error", e.getMessage());
            log.error("Failed to get cache statistics", e);
        }

        return stats;
    }

    private long countKeysByPattern(String pattern) {
        try {
            var keys = redisTemplate.keys(pattern);
            return keys != null ? keys.size() : 0;
        } catch (Exception e) {
            log.warn("Failed to count keys for pattern: {}", pattern, e);
            return 0;
        }
    }
}
