package com.bt.main.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReactiveRedisTokenService {

    private final ReactiveRedisTemplate<String, Object> reactiveRedisTemplate;
    private static final String TOKEN_PREFIX = "auth:token:";
    private static final String BLACKLIST_PREFIX = "auth:blacklist:";
    private static final Duration TOKEN_VALIDITY = Duration.ofHours(24);

    public Mono<Boolean> storeToken(String token, String username) {
        String key = TOKEN_PREFIX + token;
        return reactiveRedisTemplate.opsForValue()
                .set(key, username, TOKEN_VALIDITY)
                .doOnSuccess(success -> log.debug("Stored token for user: {}", username));
    }

    public Mono<String> getUsernameFromToken(String token) {
        String key = TOKEN_PREFIX + token;
        return reactiveRedisTemplate.opsForValue()
                .get(key)
                .map(Object::toString);
    }

    public Mono<Boolean> isTokenValid(String token) {
        String key = TOKEN_PREFIX + token;
        return reactiveRedisTemplate.hasKey(key)
                .flatMap(hasKey -> {
                    if (Boolean.TRUE.equals(hasKey)) {
                        return isTokenBlacklisted(token)
                                .map(blacklisted -> !blacklisted);
                    }
                    return Mono.just(false);
                });
    }

    public Mono<Boolean> blacklistToken(String token) {
        String tokenKey = TOKEN_PREFIX + token;
        String blacklistKey = BLACKLIST_PREFIX + token;

        return reactiveRedisTemplate.getExpire(tokenKey)
                .flatMap(ttl -> {
                    if (ttl != null && ttl.getSeconds() > 0) {
                        return reactiveRedisTemplate.opsForValue()
                                .set(blacklistKey, "true", ttl)
                                .doOnSuccess(success -> log.info("Token blacklisted successfully"));
                    }
                    return Mono.just(false);
                });
    }

    public Mono<Boolean> isTokenBlacklisted(String token) {
        String blacklistKey = BLACKLIST_PREFIX + token;
        return reactiveRedisTemplate.hasKey(blacklistKey);
    }

    public Mono<Boolean> removeToken(String token) {
        String key = TOKEN_PREFIX + token;
        return reactiveRedisTemplate.delete(key)
                .map(count -> count > 0)
                .doOnSuccess(success -> log.debug("Removed token from Redis"));
    }

    public Mono<Boolean> extendTokenValidity(String token, Duration duration) {
        String key = TOKEN_PREFIX + token;
        return reactiveRedisTemplate.expire(key, duration)
                .doOnSuccess(success -> log.debug("Extended token validity for {} hours", duration.toHours()));
    }
}
