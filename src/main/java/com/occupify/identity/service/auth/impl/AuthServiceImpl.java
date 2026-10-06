package com.occupify.identity.service.auth.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.occupify.identity.dto.request.auth.ChangePasswordRequest;
import com.occupify.identity.dto.request.auth.ForgotPasswordRequest;
import com.occupify.identity.dto.request.auth.LoginRequest;
import com.occupify.identity.dto.request.auth.RegisterRequest;
import com.occupify.identity.dto.request.auth.ResetPasswordRequest;
import com.occupify.identity.dto.request.auth.SendOtpRequest;
import com.occupify.identity.dto.request.auth.VerifyOtpRequest;
import com.occupify.identity.dto.response.auth.AuthResponse;
import com.occupify.identity.dto.response.auth.UserResponse;
import com.occupify.identity.dto.response.auth.VerifyOtpResponse;
import com.occupify.identity.entity.User;
import com.occupify.identity.enums.OtpType;
import com.occupify.identity.enums.UserStatus;
import com.occupify.identity.exception.auth.AuthErrorCode;
import com.occupify.identity.exception.auth.AuthException;
import com.occupify.identity.repository.UserRepository;
import com.occupify.identity.security.JwtUtils;
import com.occupify.identity.service.auth.AuthService;
import com.occupify.identity.service.email.EmailService;
import com.occupify.identity.util.EmailUtil;
import com.occupify.identity.util.PasswordUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    // Redis key prefixes
    private static final String KEY_PREFIX_REFRESH_TOKEN = "refresh_token:";
    private static final String KEY_PREFIX_USER_SESSIONS = "user_sessions:";
    private static final String KEY_PREFIX_REGISTER = "otp:register:";
    private static final String KEY_PREFIX_FORGOT_PASSWORD = "otp:forgot_password:";
    private static final String KEY_PREFIX_RESET_TOKEN = "password_reset_token:";
    private static final Duration RESET_TOKEN_TTL = Duration.ofMinutes(10);

    // Dependencies
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final EmailService emailService;
    private final ReactiveRedisOperations<String, String> redisTemplate;
    private final RedisScript<Long> revokeAllSessionsScript;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    // Configuration values
    private final int otpLength;
    private final Duration otpExpirationDuration;
    private final int maxAttempts;
    private final long cooldownMillis;
    private final Duration refreshTokenTtl;

    public AuthServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtUtils jwtUtils,
            EmailService emailService,
            ReactiveRedisOperations<String, String> redisTemplate,
            RedisScript<Long> revokeAllSessionsScript,
            ObjectMapper objectMapper,
            @Value("${app.otp.length:6}") int otpLength,
            @Value("${app.otp.expiration-seconds:300}") long otpExpirationSeconds,
            @Value("${app.otp.max-attempts:3}") int maxAttempts,
            @Value("${app.otp.cooldown-seconds:60}") long cooldownSeconds,
            @Value("${app.jwt.refresh-token-expiration:604800000}") long refreshTokenExpirationMs) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.emailService = emailService;
        this.redisTemplate = redisTemplate;
        this.revokeAllSessionsScript = revokeAllSessionsScript;
        this.objectMapper = objectMapper;
        this.otpLength = otpLength;
        this.otpExpirationDuration = Duration.ofSeconds(otpExpirationSeconds);
        this.maxAttempts = maxAttempts;
        this.cooldownMillis = cooldownSeconds * 1000L;
        this.refreshTokenTtl = Duration.ofMillis(refreshTokenExpirationMs);
    }

    // Records for internal Redis data structures
    private record SessionMetadata(UUID userId, String email, long createdAt) {
    }

    private record OtpData(String otpCode, int attempts, long createdAt) {
    }

    // ==========================================
    // Core Auth Operations
    // ==========================================

    @Override
    @Transactional
    public Mono<UserResponse> register(RegisterRequest request) {
        validateEmailAndPassword(request.email(), request.password());
        String normalizedEmail = EmailUtil.normalize(request.email());

        return userRepository.findByEmail(normalizedEmail)
                .flatMap(existingUser -> handleExistingUserRegistration(existingUser, request.password()))
                .switchIfEmpty(Mono.defer(() -> handleNewUserRegistration(normalizedEmail, request.password())));
    }

    @Override
    public Mono<AuthResponse> login(LoginRequest request) {
        validateEmailAndPassword(request.email(), request.password());
        String normalizedEmail = EmailUtil.normalize(request.email());

        return userRepository.findByEmail(normalizedEmail)
                .switchIfEmpty(Mono.error(new AuthException(AuthErrorCode.AUTH_003)))
                .flatMap(user -> authenticateUser(user, request.password()))
                .flatMap(this::rotateUserSessionAndRespond);
    }

    @Override
    public Mono<AuthResponse> refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return Mono.error(new AuthException(AuthErrorCode.AUTH_004));
        }

        if (!jwtUtils.validateToken(refreshToken)) {
            return Mono.error(new AuthException(AuthErrorCode.AUTH_005));
        }

        return isValidSession(refreshToken)
                .flatMap(isValid -> {
                    if (!Boolean.TRUE.equals(isValid)) {
                        return Mono.error(new AuthException(AuthErrorCode.AUTH_006));
                    }
                    return resolveUserForTokenRefresh(refreshToken);
                })
                .map(user -> {
                    String newAccessToken = jwtUtils.generateAccessToken(
                            user.getEmail(),
                            user.getId().toString(),
                            user.getRole());
                    return new AuthResponse(newAccessToken, refreshToken);
                });
    }

    @Override
    public Mono<Void> signOut(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return Mono.error(new AuthException(AuthErrorCode.AUTH_004));
        }
        return revokeSession(refreshToken).then();
    }

    @Override
    public Mono<Void> resendOtp(SendOtpRequest request) {
        EmailUtil.validate(request.email());
        String normalizedEmail = EmailUtil.normalize(request.email());
        OtpType type = request.type() != null ? request.type() : OtpType.FORGOT_PASSWORD;

        return type == OtpType.REGISTER
                ? sendRegisterOtp(normalizedEmail)
                : sendForgotPasswordOtp(normalizedEmail);
    }

    @Override
    @Transactional
    public Mono<Object> verifyOtp(VerifyOtpRequest request) {
        EmailUtil.validate(request.email());
        String normalizedEmail = EmailUtil.normalize(request.email());
        OtpType type = request.type() != null ? request.type() : OtpType.FORGOT_PASSWORD;

        return type == OtpType.REGISTER
                ? verifyRegisterOtp(normalizedEmail, request.otpCode())
                : verifyForgotPasswordOtp(normalizedEmail, request.otpCode());
    }

    @Override
    public Mono<Void> forgotPassword(ForgotPasswordRequest request) {
        EmailUtil.validate(request.email());
        String normalizedEmail = EmailUtil.normalize(request.email());
        return sendForgotPasswordOtp(normalizedEmail);
    }

    @Override
    @Transactional
    public Mono<Void> resetPassword(ResetPasswordRequest request) {
        validateEmailAndPassword(request.email(), request.newPassword());
        String normalizedEmail = EmailUtil.normalize(request.email());

        if (request.resetToken() != null && !request.resetToken().isBlank()) {
            return resetPasswordWithToken(normalizedEmail, request.resetToken(), request.newPassword());
        }
        if (request.otpCode() != null && !request.otpCode().isBlank()) {
            return resetPasswordWithOtp(normalizedEmail, request.otpCode(), request.newPassword());
        }
        return Mono.error(new AuthException(AuthErrorCode.AUTH_016));
    }

    @Override
    @Transactional
    public Mono<Void> changePassword(String userEmail, ChangePasswordRequest request) {
        if (userEmail == null || userEmail.isBlank()) {
            return Mono.error(new AuthException(AuthErrorCode.AUTH_003, "Unauthenticated user context"));
        }
        PasswordUtil.validatePasswordChange(request.currentPassword(), request.newPassword());
        String normalizedEmail = EmailUtil.normalize(userEmail);

        return userRepository.findByEmail(normalizedEmail)
                .switchIfEmpty(Mono.error(new AuthException(AuthErrorCode.USER_001)))
                .flatMap(user -> verifyCurrentPassword(user, request.currentPassword()))
                .flatMap(user -> updatePasswordAndInvalidateSessions(normalizedEmail, request.newPassword()));
    }

    // ==========================================
    // Registration Helpers
    // ==========================================

    private Mono<UserResponse> handleExistingUserRegistration(User existingUser, String newPassword) {
        if (UserStatus.ACTIVE.name().equalsIgnoreCase(existingUser.getStatus())) {
            return Mono.error(new AuthException(AuthErrorCode.AUTH_001));
        }
        if (UserStatus.BANNED.name().equalsIgnoreCase(existingUser.getStatus())) {
            return Mono.error(new AuthException(AuthErrorCode.USER_008));
        }
        String encodedPassword = passwordEncoder.encode(newPassword);
        return userRepository.updatePasswordByEmail(existingUser.getEmail(), encodedPassword)
                .filter(rows -> rows > 0)
                .switchIfEmpty(Mono.error(new AuthException(AuthErrorCode.USER_001, "Failed to update user credentials")))
                .flatMap(rows -> generateAndStoreOtp(existingUser.getEmail(), OtpType.REGISTER))
                .flatMap(rawOtp -> emailService.sendRegistrationOtp(existingUser.getEmail(), rawOtp))
                .thenReturn(mapToUserResponse(existingUser));
    }

    private Mono<UserResponse> handleNewUserRegistration(String email, String rawPassword) {
        String encodedPassword = passwordEncoder.encode(rawPassword);
        User newUser = User.createInactive(email, encodedPassword);
        return userRepository.save(newUser)
                .flatMap(savedUser -> generateAndStoreOtp(email, OtpType.REGISTER)
                        .flatMap(rawOtp -> emailService.sendRegistrationOtp(email, rawOtp))
                        .thenReturn(mapToUserResponse(savedUser)));
    }

    private Mono<Void> sendRegisterOtp(String email) {
        return userRepository.findByEmail(email)
                .switchIfEmpty(Mono.error(new AuthException(AuthErrorCode.AUTH_012)))
                .flatMap(this::validateRegistrationEligibility)
                .flatMap(user -> generateAndStoreOtp(email, OtpType.REGISTER))
                .flatMap(rawOtp -> emailService.sendRegistrationOtp(email, rawOtp));
    }

    private Mono<Void> sendForgotPasswordOtp(String email) {
        return userRepository.findByEmail(email)
                .switchIfEmpty(Mono.error(new AuthException(AuthErrorCode.AUTH_012)))
                .flatMap(this::validateUserStatus)
                .flatMap(user -> generateAndStoreOtp(email, OtpType.FORGOT_PASSWORD))
                .flatMap(rawOtp -> emailService.sendPasswordResetOtp(email, rawOtp));
    }

    // ==========================================
    // OTP Verification Helpers
    // ==========================================

    private Mono<Object> verifyRegisterOtp(String email, String otpCode) {
        return userRepository.findByEmail(email)
                .switchIfEmpty(Mono.error(new AuthException(AuthErrorCode.AUTH_012)))
                .flatMap(this::validateRegistrationEligibility)
                .flatMap(user -> verifyOtpInternal(email, otpCode, OtpType.REGISTER)
                        .then(userRepository.updateStatusByEmail(email, UserStatus.ACTIVE.name()))
                        .filter(rows -> rows > 0)
                        .switchIfEmpty(Mono.error(new AuthException(AuthErrorCode.USER_001, "Failed to activate user account")))
                        .flatMap(rows -> {
                            user.setStatus(UserStatus.ACTIVE.name());
                            return generateAuthResponse(user).map(auth -> (Object) auth);
                        }));
    }

    private Mono<Object> verifyForgotPasswordOtp(String email, String otpCode) {
        return userRepository.findByEmail(email)
                .switchIfEmpty(Mono.error(new AuthException(AuthErrorCode.AUTH_012)))
                .flatMap(this::validateUserStatus)
                .flatMap(user -> verifyOtpInternal(email, otpCode, OtpType.FORGOT_PASSWORD))
                .then(createPasswordResetToken(email))
                .map(resetToken -> (Object) new VerifyOtpResponse(resetToken));
    }

    private Mono<Void> resetPasswordWithToken(String email, String resetToken, String newPassword) {
        return validatePasswordResetToken(email, resetToken)
                .then(userRepository.findByEmail(email))
                .switchIfEmpty(Mono.error(new AuthException(AuthErrorCode.AUTH_012)))
                .flatMap(user -> updatePasswordAndInvalidateSessions(email, newPassword))
                .then(deletePasswordResetToken(resetToken));
    }

    private Mono<Void> resetPasswordWithOtp(String email, String otpCode, String newPassword) {
        return verifyOtpInternal(email, otpCode, OtpType.FORGOT_PASSWORD)
                .then(userRepository.findByEmail(email))
                .switchIfEmpty(Mono.error(new AuthException(AuthErrorCode.AUTH_012)))
                .flatMap(user -> updatePasswordAndInvalidateSessions(email, newPassword));
    }

    // ==========================================
    // Session & Token Helpers
    // ==========================================

    private Mono<AuthResponse> generateAuthResponse(User user) {
        String accessToken = jwtUtils.generateAccessToken(user.getEmail(), user.getId().toString(), user.getRole());
        String refreshToken = jwtUtils.generateRefreshToken(user.getEmail());

        return saveSession(refreshToken, user.getId(), user.getEmail())
                .thenReturn(new AuthResponse(accessToken, refreshToken, mapToUserResponse(user)));
    }

    private Mono<AuthResponse> rotateUserSessionAndRespond(User user) {
        return revokeAllUserSessions(user.getEmail())
                .then(generateAuthResponse(user));
    }

    private Mono<User> authenticateUser(User user, String rawPassword) {
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            return Mono.error(new AuthException(AuthErrorCode.AUTH_003));
        }
        return validateUserStatus(user);
    }

    private Mono<User> validateUserStatus(User user) {
        if (UserStatus.INACTIVE.name().equalsIgnoreCase(user.getStatus())) {
            return Mono.error(new AuthException(AuthErrorCode.USER_007));
        }
        if (UserStatus.BANNED.name().equalsIgnoreCase(user.getStatus())) {
            return Mono.error(new AuthException(AuthErrorCode.USER_008));
        }
        return Mono.just(user);
    }

    private Mono<User> validateRegistrationEligibility(User user) {
        if (UserStatus.ACTIVE.name().equalsIgnoreCase(user.getStatus())) {
            return Mono.error(new AuthException(AuthErrorCode.AUTH_015));
        }
        if (UserStatus.BANNED.name().equalsIgnoreCase(user.getStatus())) {
            return Mono.error(new AuthException(AuthErrorCode.USER_008));
        }
        return Mono.just(user);
    }

    private Mono<User> resolveUserForTokenRefresh(String refreshToken) {
        String email = jwtUtils.extractEmail(refreshToken);
        return userRepository.findByEmail(email)
                .switchIfEmpty(Mono.error(new AuthException(AuthErrorCode.AUTH_003)))
                .flatMap(this::validateUserStatus);
    }

    private Mono<User> verifyCurrentPassword(User user, String currentPassword) {
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            return Mono.error(new AuthException(AuthErrorCode.AUTH_013));
        }
        return Mono.just(user);
    }

    private Mono<Void> updatePasswordAndInvalidateSessions(String email, String newPassword) {
        String encodedPassword = passwordEncoder.encode(newPassword);
        return userRepository.updatePasswordByEmail(email, encodedPassword)
                .filter(rows -> rows > 0)
                .switchIfEmpty(Mono.error(new AuthException(AuthErrorCode.USER_001, "Failed to update user password")))
                .flatMap(rows -> revokeAllUserSessions(email))
                .then();
    }

    private UserResponse mapToUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt());
    }

    private void validateEmailAndPassword(String email, String password) {
        EmailUtil.validate(email);
        PasswordUtil.validate(password);
    }

    // ==========================================
    // Redis Session Management Logic
    // ==========================================

    private Mono<Void> saveSession(String refreshToken, UUID userId, String email) {
        if (refreshToken == null || userId == null || email == null) {
            return Mono.error(new IllegalArgumentException("Session parameters must not be null"));
        }

        String tokenKey = buildTokenKey(refreshToken);
        String sessionKey = buildUserSessionsKey(email);
        SessionMetadata metadata = new SessionMetadata(userId, email, System.currentTimeMillis());

        try {
            String jsonPayload = objectMapper.writeValueAsString(metadata);
            return redisTemplate.opsForValue().set(tokenKey, jsonPayload, refreshTokenTtl)
                    .then(redisTemplate.opsForSet().add(sessionKey, tokenKey))
                    .then(redisTemplate.expire(sessionKey, refreshTokenTtl))
                    .then();
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize session metadata for user [{}]", userId, e);
            return Mono.error(e);
        }
    }

    private Mono<Boolean> isValidSession(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return Mono.just(false);
        }
        return redisTemplate.hasKey(buildTokenKey(refreshToken));
    }

    private Mono<SessionMetadata> getSession(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return Mono.empty();
        }
        return redisTemplate.opsForValue().get(buildTokenKey(refreshToken))
                .flatMap(this::deserializeSessionMetadata);
    }

    private Mono<Void> revokeSession(String refreshToken) {
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

    private Mono<Long> revokeAllUserSessions(String email) {
        if (email == null || email.isBlank()) {
            return Mono.just(0L);
        }
        String sessionKey = buildUserSessionsKey(email);
        List<String> keys = List.of(sessionKey);

        return redisTemplate.execute(revokeAllSessionsScript, keys)
                .next()
                .defaultIfEmpty(0L)
                .doOnSuccess(count -> log.info("Revoked {} active sessions for user [{}]", count, EmailUtil.mask(email)));
    }

    private Mono<SessionMetadata> deserializeSessionMetadata(String json) {
        try {
            return Mono.just(objectMapper.readValue(json, SessionMetadata.class));
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize session metadata from Redis: {}", e.getMessage(), e);
            return Mono.error(new AuthException(AuthErrorCode.AUTH_005, "Corrupted session data format"));
        }
    }

    private String buildTokenKey(String token) {
        return KEY_PREFIX_REFRESH_TOKEN + token;
    }

    private String buildUserSessionsKey(String email) {
        return KEY_PREFIX_USER_SESSIONS + email.trim().toLowerCase();
    }

    // ==========================================
    // Redis OTP Management Logic
    // ==========================================

    private Mono<String> generateAndStoreOtp(String email, OtpType type) {
        if (email == null || email.isBlank()) {
            return Mono.error(new AuthException(AuthErrorCode.AUTH_000));
        }
        OtpType resolvedType = type != null ? type : OtpType.FORGOT_PASSWORD;
        String key = buildOtpKey(email, resolvedType);
        return fetchExistingOtp(key)
                .flatMap(this::checkCooldown)
                .then(Mono.defer(() -> createAndSaveOtp(key, email, resolvedType)));
    }

    private Mono<Void> verifyOtpInternal(String email, String rawOtp, OtpType type) {
        if (email == null || email.isBlank() || rawOtp == null || rawOtp.isBlank()) {
            return Mono.error(new AuthException(AuthErrorCode.AUTH_009));
        }
        OtpType resolvedType = type != null ? type : OtpType.FORGOT_PASSWORD;
        String key = buildOtpKey(email, resolvedType);
        return fetchExistingOtp(key)
                .switchIfEmpty(Mono.error(new AuthException(AuthErrorCode.AUTH_008)))
                .flatMap(otpData -> processOtpVerification(key, otpData, rawOtp));
    }

    private Mono<String> createPasswordResetToken(String email) {
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

    private Mono<String> validatePasswordResetToken(String email, String resetToken) {
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

    private Mono<Void> deletePasswordResetToken(String resetToken) {
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
            return redisTemplate.opsForValue().set(key, json, otpExpirationDuration)
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
                    .defaultIfEmpty(otpExpirationDuration)
                    .flatMap(ttl -> {
                        Duration safeTtl = (ttl != null && !ttl.isNegative() && !ttl.isZero()) ? ttl : otpExpirationDuration;
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
