package com.bt.fixeddeposit.controller;

import com.bt.fixeddeposit.service.RedisHealthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/redis")
@RequiredArgsConstructor
public class RedisHealthController {

    private final RedisHealthService redisHealthService;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getRedisHealth() {
        return ResponseEntity.ok(redisHealthService.getHealthStatus());
    }

    @GetMapping("/cache/stats")
    public ResponseEntity<Map<String, Object>> getCacheStatistics() {
        return ResponseEntity.ok(redisHealthService.getCacheStatistics());
    }
}
