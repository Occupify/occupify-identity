package com.occupify.identity.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.occupify.identity.exception.AuthErrorCode;
import com.occupify.identity.exception.AuthException;
import com.occupify.identity.service.OtpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.security.SecureRandom;
import java.time.Duration;

@Service
public class RedisOtpServiceImpl implements OtpService {

    private static final Logger log = LoggerFactory.getLogger(RedisOtpServiceImpl.class);
    private static final String KEY_PREFIX_OTP = "password_reset_otp:";

    private final ReactiveRedisOperations<String, String> redisTemplate;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    private final int otpLength;
    private final Duration expirationDuration;
    private final int maxAttempts;
    private final long cooldownMillis;

    public RedisOtpServiceImpl(
            ReactiveRedisOperations<String, String> redisTemplate,
            PasswordEncoder passwordEncoder,
            ObjectMapper objectMapper,
            @Value("${app.otp.length:6}") int otpLength,
            @Value("${app.otp.expiration-seconds:300}") long expirationSeconds,
            @Value("${app.otp.max-attempts:3}") int maxAttempts,
            @Value("${app.otp.cooldown-seconds:60}") long cooldownSeconds) {
        this.redisTemplate = redisTemplate;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
        this.otpLength = otpLength;
        this.expirationDuration = Duration.ofSeconds(expirationSeconds);
        this.maxAttempts = maxAttempts;
        this.cooldownMillis = cooldownSeconds * 1000L;
    }

    @Override
    public Mono<String> generateAndStoreOtp(String email) {
        if (email == null || email.isBlank()) {
            return Mono.error(new AuthException(AuthErrorCode.AUTH_000));
        }

        String key = buildOtpKey(email);
        return fetchExistingOtp(key)
                .flatMap(this::checkCooldown)
                .then(Mono.defer(() -> createAndSaveOtp(key, email)));
    }

    @Override
    public Mono<Void> verifyOtp(String email, String rawOtp) {
        if (email == null || email.isBlank() || rawOtp == null || rawOtp.isBlank()) {
            return Mono.error(new AuthException(AuthErrorCode.AUTH_009));
        }

        String key = buildOtpKey(email);
        return fetchExistingOtp(key)
                .switchIfEmpty(Mono.error(new AuthException(AuthErrorCode.AUTH_008)))
                .flatMap(otpData -> processOtpVerification(key, otpData, rawOtp));
    }

    private Mono<Void> checkCooldown(OtpData existingData) {
        long elapsed = System.currentTimeMillis() - existingData.createdAt();
        if (elapsed < cooldownMillis) {
            return Mono.error(new AuthException(AuthErrorCode.AUTH_007));
        }
        return Mono.empty();
    }

    private Mono<String> createAndSaveOtp(String key, String email) {
        String rawOtp = generateNumericOtp(otpLength);
        String hashedOtp = passwordEncoder.encode(rawOtp);
        OtpData otpData = new OtpData(hashedOtp, 0, System.currentTimeMillis());

        try {
            String json = objectMapper.writeValueAsString(otpData);
            return redisTemplate.opsForValue().set(key, json, expirationDuration)
                    .doOnSuccess(v -> log.info("Generated password reset OTP for user [{}]", maskEmail(email)))
                    .thenReturn(rawOtp);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize OTP data for [{}]: {}", maskEmail(email), e.getMessage());
            return Mono.error(new AuthException(AuthErrorCode.AUTH_011));
        }
    }

    private Mono<Void> processOtpVerification(String key, OtpData otpData, String rawOtp) {
        if (otpData.attempts() >= maxAttempts) {
            return redisTemplate.delete(key)
                    .then(Mono.error(new AuthException(AuthErrorCode.AUTH_010)));
        }

        boolean matches = passwordEncoder.matches(rawOtp, otpData.otpCode());
        if (!matches) {
            int newAttempts = otpData.attempts() + 1;
            if (newAttempts >= maxAttempts) {
                return redisTemplate.delete(key)
                        .then(Mono.error(new AuthException(AuthErrorCode.AUTH_010)));
            }
            return updateAttempts(key, otpData, newAttempts)
                    .then(Mono.error(new AuthException(AuthErrorCode.AUTH_009,
                            "Invalid OTP code. " + (maxAttempts - newAttempts) + " attempts remaining.")));
        }

        return redisTemplate.delete(key).then();
    }

    private Mono<Void> updateAttempts(String key, OtpData otpData, int newAttempts) {
        OtpData updated = new OtpData(otpData.otpCode(), newAttempts, otpData.createdAt());
        try {
            String json = objectMapper.writeValueAsString(updated);
            return redisTemplate.getExpire(key)
                    .flatMap(remainingTtl -> redisTemplate.opsForValue().set(key, json, remainingTtl))
                    .then();
        } catch (JsonProcessingException e) {
            log.error("Failed to update OTP attempt count in Redis: {}", e.getMessage());
            return Mono.empty();
        }
    }

    private Mono<OtpData> fetchExistingOtp(String key) {
        return redisTemplate.opsForValue().get(key)
                .flatMap(json -> {
                    try {
                        return Mono.just(objectMapper.readValue(json, OtpData.class));
                    } catch (JsonProcessingException e) {
                        log.error("Failed to deserialize OTP data from Redis: {}", e.getMessage());
                        return Mono.empty();
                    }
                });
    }

    private String generateNumericOtp(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(secureRandom.nextInt(10));
        }
        return sb.toString();
    }

    private String buildOtpKey(String email) {
        return KEY_PREFIX_OTP + email.trim().toLowerCase();
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        int atIndex = email.indexOf('@');
        return email.charAt(0) + "***" + email.substring(atIndex);
    }
}
