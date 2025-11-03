package com.bt.accounts.event;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Stores pending requests and their responses for correlation.
 * Used for request-response pattern in Kafka communication.
 */
@Service
public class RequestResponseStore {
    private final Map<String, Object> store = new ConcurrentHashMap<>();
    private final Map<String, Long> timestamps = new ConcurrentHashMap<>();
    private static final Object PENDING = new Object(); // Sentinel value for pending requests

    public void putRequest(String requestId, Object request) {
        // Use sentinel value to mark request as pending
        store.put(requestId, PENDING);
        timestamps.put(requestId, System.currentTimeMillis());
    }

    public Object getResponse(String requestId, long timeout, TimeUnit unit) throws InterruptedException {
        long endTime = System.currentTimeMillis() + unit.toMillis(timeout);
        while (System.currentTimeMillis() < endTime) {
            Object response = store.get(requestId);
            // Check if we have a real response (not null and not the pending sentinel)
            if (response != null && response != PENDING) {
                store.remove(requestId);
                timestamps.remove(requestId);
                return response;
            }
            Thread.sleep(100);
        }
        // Timeout - cleanup and return null
        store.remove(requestId);
        timestamps.remove(requestId);
        return null;
    }

    public void putResponse(String requestId, Object response) {
        store.put(requestId, response);
    }

    public void removeRequest(String requestId) {
        store.remove(requestId);
        timestamps.remove(requestId);
    }

    public boolean hasRequest(String requestId) {
        return store.containsKey(requestId);
    }

    /**
     * Cleans up stale requests older than the specified timeout
     */
    public void cleanup(long timeout, TimeUnit unit) {
        long currentTime = System.currentTimeMillis();
        long timeoutMs = unit.toMillis(timeout);
        timestamps.entrySet().removeIf(entry -> (currentTime - entry.getValue()) > timeoutMs);
    }
}
