package com.bt.fixeddeposit.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class RedisRequestResponseStore {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    private static final String REQUEST_PREFIX = "kafka:request:";
    private static final String RESPONSE_PREFIX = "kafka:response:";
    private static final String PENDING_MARKER = "PENDING";
    private static final long DEFAULT_TTL_SECONDS = 60;

    public RedisRequestResponseStore(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public void putRequest(String requestId, Object request) {
        String key = REQUEST_PREFIX + requestId;
        stringRedisTemplate.opsForValue().set(key, PENDING_MARKER, DEFAULT_TTL_SECONDS, TimeUnit.SECONDS);
        log.debug("Stored pending request: {}", requestId);
    }

    public <T> T getResponse(String requestId, Class<T> responseType, long timeout, TimeUnit unit)
            throws InterruptedException {
        String responseKey = RESPONSE_PREFIX + requestId;
        long endTime = System.currentTimeMillis() + unit.toMillis(timeout);

        while (System.currentTimeMillis() < endTime) {
            String responseJson = stringRedisTemplate.opsForValue().get(responseKey);
            if (responseJson != null) {
                stringRedisTemplate.delete(responseKey);
                stringRedisTemplate.delete(REQUEST_PREFIX + requestId);
                log.debug("Retrieved response for request: {}", requestId);
                try {
                    return objectMapper.readValue(responseJson, responseType);
                } catch (Exception e) {
                    log.error("Failed to deserialize response", e);
                    return null;
                }
            }
            Thread.sleep(100);
        }

        stringRedisTemplate.delete(REQUEST_PREFIX + requestId);
        log.warn("Request timed out: {}", requestId);
        return null;
    }

    public void putResponse(String requestId, Object response) {
        String key = RESPONSE_PREFIX + requestId;
        try {
            String json = objectMapper.writeValueAsString(response);
            stringRedisTemplate.opsForValue().set(key, json, DEFAULT_TTL_SECONDS, TimeUnit.SECONDS);
            log.debug("Stored response for request: {}", requestId);
        } catch (Exception e) {
            log.error("Failed to serialize response", e);
        }
    }

    public void removeRequest(String requestId) {
        stringRedisTemplate.delete(REQUEST_PREFIX + requestId);
        stringRedisTemplate.delete(RESPONSE_PREFIX + requestId);
        log.debug("Removed request: {}", requestId);
    }

    public boolean hasRequest(String requestId) {
        String key = REQUEST_PREFIX + requestId;
        Boolean exists = stringRedisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }
}
