package com.occupify.identity.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.occupify.identity.service.SessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
public class RedisSessionServiceImpl implements SessionService {

    private static final Logger log = LoggerFactory.getLogger(RedisSessionServiceImpl.class);

    private static final String KEY_PREFIX_REFRESH_TOKEN = "refresh_token:";
    private static final String KEY_PREFIX_USER_SESSIONS = "user_sessions:";

    private final ReactiveRedisOperations<String, String> redisTemplate;
    private final RedisScript<Long> revokeAllSessionsScript;
    private final ObjectMapper objectMapper;
    private final Duration tokenTtl;

    public RedisSessionServiceImpl(
            ReactiveRedisOperations<String, String> redisTemplate,
            RedisScript<Long> revokeAllSessionsScript,
            ObjectMapper objectMapper,
            @Value("${app.jwt.refresh-token-expiration:604800000}") long refreshTokenExpirationMs) {
        this.redisTemplate = redisTemplate;
        this.revokeAllSessionsScript = revokeAllSessionsScript;
        this.objectMapper = objectMapper;
        this.tokenTtl = Duration.ofMillis(refreshTokenExpirationMs);
    }

    @Override
    public Mono<Void> saveSession(String refreshToken, UUID userId, String email) {
        if (refreshToken == null || userId == null || email == null) {
            return Mono.error(new IllegalArgumentException("Session parameters must not be null"));
        }

        String tokenKey = buildTokenKey(refreshToken);
        String sessionKey = buildUserSessionsKey(email);
        SessionMetadata metadata = new SessionMetadata(userId, email, System.currentTimeMillis());

        try {
            String jsonPayload = objectMapper.writeValueAsString(metadata);
            return redisTemplate.opsForValue().set(tokenKey, jsonPayload, tokenTtl)
                    .then(redisTemplate.opsForSet().add(sessionKey, tokenKey))
                    .then(redisTemplate.expire(sessionKey, tokenTtl))
                    .then();
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize session metadata for user [{}]", userId, e);
            return Mono.error(e);
        }
    }

    @Override
    public Mono<Boolean> isValidSession(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return Mono.just(false);
        }
        return redisTemplate.hasKey(buildTokenKey(refreshToken));
    }

    @Override
    public Mono<SessionMetadata> getSession(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return Mono.empty();
        }
        return redisTemplate.opsForValue().get(buildTokenKey(refreshToken))
                .flatMap(this::deserializeMetadata);
    }

    @Override
    public Mono<Void> revokeSession(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return Mono.empty();
        }
        String tokenKey = buildTokenKey(refreshToken);

        return getSession(refreshToken)
                .flatMap(metadata -> {
                    String sessionKey = buildUserSessionsKey(metadata.email());
                    return redisTemplate.opsForSet().remove(sessionKey, tokenKey);
                })
                .then(redisTemplate.delete(tokenKey))
                .then();
    }

    @Override
    public Mono<Long> revokeAllUserSessions(String email) {
        if (email == null || email.isBlank()) {
            return Mono.just(0L);
        }
        String sessionKey = buildUserSessionsKey(email);
        List<String> keys = List.of(sessionKey);

        return redisTemplate.execute(revokeAllSessionsScript, keys)
                .next()
                .defaultIfEmpty(0L)
                .doOnSuccess(count -> log.info("Revoked {} active sessions for user [{}]", count, maskEmail(email)));
    }

    private Mono<SessionMetadata> deserializeMetadata(String json) {
        try {
            return Mono.just(objectMapper.readValue(json, SessionMetadata.class));
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize session metadata from Redis: {}", e.getMessage());
            return Mono.empty();
        }
    }

    private String buildTokenKey(String token) {
        return KEY_PREFIX_REFRESH_TOKEN + token;
    }

    private String buildUserSessionsKey(String email) {
        return KEY_PREFIX_USER_SESSIONS + email.trim().toLowerCase();
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        int atIndex = email.indexOf('@');
        return email.charAt(0) + "***" + email.substring(atIndex);
    }
}
