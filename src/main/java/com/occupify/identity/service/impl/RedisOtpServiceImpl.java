package com.occupify.identity.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.occupify.identity.enums.OtpType;
import com.occupify.identity.exception.AuthErrorCode;
import com.occupify.identity.exception.AuthException;
import com.occupify.identity.service.OtpService;
import com.occupify.identity.util.EmailUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
public class RedisOtpServiceImpl implements OtpService {

    private static final String KEY_PREFIX_REGISTER = "otp:register:";
    private static final String KEY_PREFIX_FORGOT_PASSWORD = "otp:forgot_password:";
    private static final String KEY_PREFIX_RESET_TOKEN = "password_reset_token:";
    private static final Duration RESET_TOKEN_TTL = Duration.ofMinutes(10);

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
    public Mono<String> generateAndStoreOtp(String email, OtpType type) {
        if (email == null || email.isBlank()) {
            return Mono.error(new AuthException(AuthErrorCode.AUTH_000));
        }
        OtpType resolvedType = type != null ? type : OtpType.FORGOT_PASSWORD;
        String key = buildOtpKey(email, resolvedType);
        return fetchExistingOtp(key)
                .flatMap(this::checkCooldown)
                .then(Mono.defer(() -> createAndSaveOtp(key, email, resolvedType)));
    }

    @Override
    public Mono<Void> verifyOtp(String email, String rawOtp, OtpType type) {
        if (email == null || email.isBlank() || rawOtp == null || rawOtp.isBlank()) {
            return Mono.error(new AuthException(AuthErrorCode.AUTH_009));
        }
        OtpType resolvedType = type != null ? type : OtpType.FORGOT_PASSWORD;
        String key = buildOtpKey(email, resolvedType);
        return fetchExistingOtp(key)
                .switchIfEmpty(Mono.error(new AuthException(AuthErrorCode.AUTH_008)))
                .flatMap(otpData -> processOtpVerification(key, otpData, rawOtp));
    }

    @Override
    public Mono<String> createPasswordResetToken(String email) {
        if (email == null || email.isBlank()) {
            return Mono.error(new AuthException(AuthErrorCode.AUTH_000));
        }
        String normalizedEmail = email.trim().toLowerCase();
        String resetToken = UUID.randomUUID().toString();
        String key = KEY_PREFIX_RESET_TOKEN + resetToken;

        return redisTemplate.opsForValue()
                .set(key, normalizedEmail, RESET_TOKEN_TTL)
                .doOnSuccess(v -> log.info("Generated reset token for user [{}]", EmailUtil.mask(normalizedEmail)))
                .thenReturn(resetToken);
    }

    @Override
    public Mono<String> validatePasswordResetToken(String email, String resetToken) {
        if (resetToken == null || resetToken.isBlank() || email == null || email.isBlank()) {
            return Mono.error(new AuthException(AuthErrorCode.AUTH_016));
        }
        String normalizedEmail = email.trim().toLowerCase();
        String key = KEY_PREFIX_RESET_TOKEN + resetToken.trim();

        return redisTemplate.opsForValue().get(key)
                .switchIfEmpty(Mono.error(new AuthException(AuthErrorCode.AUTH_016)))
                .flatMap(storedEmail -> {
                    if (!normalizedEmail.equalsIgnoreCase(storedEmail)) {
                        log.warn("Reset token email mismatch: expected [{}], found [{}]",
                                EmailUtil.mask(normalizedEmail), EmailUtil.mask(storedEmail));
                        return Mono.error(new AuthException(AuthErrorCode.AUTH_016));
                    }
                    return Mono.just(storedEmail);
                });
    }

    @Override
    public Mono<Void> deletePasswordResetToken(String resetToken) {
        if (resetToken == null || resetToken.isBlank()) {
            return Mono.empty();
        }
        return redisTemplate.delete(KEY_PREFIX_RESET_TOKEN + resetToken.trim()).then();
    }

    private Mono<Void> checkCooldown(OtpData existingData) {
        long elapsed = System.currentTimeMillis() - existingData.createdAt();
        if (elapsed < cooldownMillis) {
            return Mono.error(new AuthException(AuthErrorCode.AUTH_007));
        }
        return Mono.empty();
    }

    private Mono<String> createAndSaveOtp(String key, String email, OtpType type) {
        String rawOtp = generateNumericOtp(otpLength);
        String hashedOtp = passwordEncoder.encode(rawOtp);
        OtpData otpData = new OtpData(hashedOtp, 0, System.currentTimeMillis());

        try {
            String json = objectMapper.writeValueAsString(otpData);
            return redisTemplate.opsForValue().set(key, json, expirationDuration)
                    .doOnSuccess(v -> log.info("Generated {} OTP for user [{}]", type, EmailUtil.mask(email)))
                    .thenReturn(rawOtp);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize OTP data for [{}]: {}", EmailUtil.mask(email), e.getMessage(), e);
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
                    .defaultIfEmpty(expirationDuration)
                    .flatMap(ttl -> {
                        Duration safeTtl = (ttl != null && !ttl.isNegative() && !ttl.isZero()) ? ttl : expirationDuration;
                        return redisTemplate.opsForValue().set(key, json, safeTtl);
                    })
                    .then();
        } catch (JsonProcessingException e) {
            log.error("Failed to update OTP attempt count in Redis: {}", e.getMessage(), e);
            return Mono.error(new AuthException(AuthErrorCode.AUTH_011, "Failed to update OTP attempts state"));
        }
    }

    private Mono<OtpData> fetchExistingOtp(String key) {
        return redisTemplate.opsForValue().get(key)
                .flatMap(json -> {
                    try {
                        return Mono.just(objectMapper.readValue(json, OtpData.class));
                    } catch (JsonProcessingException e) {
                        log.error("Failed to deserialize OTP data from Redis for key [{}]: {}", key, e.getMessage(), e);
                        return Mono.error(new AuthException(AuthErrorCode.AUTH_011, "Corrupted OTP data format"));
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

    private String buildOtpKey(String email, OtpType type) {
        String prefix = type == OtpType.REGISTER ? KEY_PREFIX_REGISTER : KEY_PREFIX_FORGOT_PASSWORD;
        return prefix + email.trim().toLowerCase();
    }

}
