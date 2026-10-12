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
import com.occupify.identity.event.PasswordResetRequestedEvent;
import com.occupify.identity.event.UserRegisteredEvent;
import com.occupify.identity.mapper.UserMapper;
import com.occupify.identity.producer.UserEventProducer;
import com.occupify.identity.repository.UserRepository;
import com.occupify.identity.security.JwtUtils;
import com.occupify.identity.service.auth.AuthService;
import com.occupify.identity.util.EmailUtil;
import com.occupify.identity.util.OtpUtils;
import com.occupify.identity.util.PasswordUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.lang.Nullable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.occupify.identity.constant.RedisConstants.*;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private static final Duration RESET_TOKEN_TTL = Duration.ofMinutes(10);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final StringRedisTemplate redisTemplate;
    private final RedisScript<Long> revokeAllSessionsScript;
    private final ObjectMapper objectMapper;
    private final OtpUtils otpUtils;
    private final UserMapper userMapper;
    private final UserEventProducer userEventProducer;

    private final int otpLength;
    private final Duration otpExpirationDuration;
    private final int maxAttempts;
    private final long cooldownMillis;
    private final Duration refreshTokenTtl;

    @Autowired
    public AuthServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtUtils jwtUtils,
            StringRedisTemplate redisTemplate,
            RedisScript<Long> revokeAllSessionsScript,
            ObjectMapper objectMapper,
            OtpUtils otpUtils,
            UserMapper userMapper,
            UserEventProducer userEventProducer,
            @Value("${app.otp.length:6}") int otpLength,
            @Value("${app.otp.expiration-seconds:300}") long otpExpirationSeconds,
            @Value("${app.otp.max-attempts:3}") int maxAttempts,
            @Value("${app.otp.cooldown-seconds:60}") long cooldownSeconds,
            @Value("${app.jwt.refresh-token-expiration:604800000}") long refreshTokenExpirationMs) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.redisTemplate = redisTemplate;
        this.revokeAllSessionsScript = revokeAllSessionsScript;
        this.objectMapper = objectMapper;
        this.otpUtils = otpUtils;
        this.userMapper = userMapper;
        this.userEventProducer = userEventProducer;
        this.otpLength = otpLength;
        this.otpExpirationDuration = Duration.ofSeconds(otpExpirationSeconds);
        this.maxAttempts = maxAttempts;
        this.cooldownMillis = cooldownSeconds * 1000L;
        this.refreshTokenTtl = Duration.ofMillis(refreshTokenExpirationMs);
    }

    public AuthServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtUtils jwtUtils,
            StringRedisTemplate redisTemplate,
            RedisScript<Long> revokeAllSessionsScript,
            ObjectMapper objectMapper,
            UserEventProducer userEventProducer,
            int otpLength,
            long otpExpirationSeconds,
            int maxAttempts,
            long cooldownSeconds,
            long refreshTokenExpirationMs) {
        this(userRepository, passwordEncoder, jwtUtils, redisTemplate,
                revokeAllSessionsScript, objectMapper, new OtpUtils(), new UserMapper(), userEventProducer,
                otpLength, otpExpirationSeconds, maxAttempts, cooldownSeconds, refreshTokenExpirationMs);
    }

    private record SessionMetadata(UUID userId, String email, long createdAt) {
    }

    private record OtpData(String otpCode, int attempts, long createdAt) {
    }

    // ==========================================
    // Core Auth Operations
    // ==========================================

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        validateEmailAndPassword(request.email(), request.password());
        String normalizedEmail = EmailUtil.normalize(request.email());

        Optional<User> existingUserOpt = userRepository.findByEmail(normalizedEmail);
        if (existingUserOpt.isPresent()) {
            return handleExistingUserRegistration(existingUserOpt.get(), request.password());
        } else {
            return handleNewUserRegistration(normalizedEmail, request.password());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        validateEmailAndPassword(request.email(), request.password());
        String normalizedEmail = EmailUtil.normalize(request.email());

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new AuthException(AuthErrorCode.AUTH_003));
        authenticateUser(user, request.password());
        return rotateUserSessionAndRespond(user);
    }

    @Override
    public AuthResponse refreshToken(String rawRefreshToken) {
        String refreshToken = cleanToken(rawRefreshToken);
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new AuthException(AuthErrorCode.AUTH_004);
        }

        try {
            jwtUtils.parseClaims(refreshToken);
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            log.warn("Refresh token expired: {}", e.getMessage());
            throw new AuthException(AuthErrorCode.AUTH_006);
        } catch (Exception e) {
            log.warn("Invalid refresh token format or signature: {}", e.getMessage());
            throw new AuthException(AuthErrorCode.AUTH_005);
        }

        if (!isValidSession(refreshToken)) {
            log.warn("Session invalid or revoked in Redis for token key: [{}]", buildTokenKey(refreshToken));
            throw new AuthException(AuthErrorCode.AUTH_006);
        }

        User user = resolveUserForTokenRefresh(refreshToken);
        String newAccessToken = jwtUtils.generateAccessToken(
                user.getEmail(),
                user.getId().toString(),
                user.getRole());
        String newRefreshToken = jwtUtils.generateRefreshToken(user.getEmail());

        revokeSession(refreshToken);
        saveSession(newRefreshToken, user.getId(), user.getEmail());
        return new AuthResponse(newAccessToken, newRefreshToken, mapToUserResponse(user));
    }

    @Override
    public void signOut(String rawRefreshToken) {
        String refreshToken = cleanToken(rawRefreshToken);
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new AuthException(AuthErrorCode.AUTH_004);
        }
        revokeSession(refreshToken);
    }

    @Override
    public void resendOtp(SendOtpRequest request) {
        EmailUtil.validate(request.email());
        String normalizedEmail = EmailUtil.normalize(request.email());
        OtpType type = request.type() != null ? request.type() : OtpType.FORGOT_PASSWORD;

        if (type == OtpType.REGISTER) {
            sendRegisterOtp(normalizedEmail);
        } else {
            sendForgotPasswordOtp(normalizedEmail);
        }
    }

    @Override
    @Transactional
    public Object verifyOtp(VerifyOtpRequest request) {
        EmailUtil.validate(request.email());
        String normalizedEmail = EmailUtil.normalize(request.email());
        OtpType type = request.type() != null ? request.type() : OtpType.FORGOT_PASSWORD;

        if (type == OtpType.REGISTER) {
            return verifyRegisterOtp(normalizedEmail, request.otpCode());
        } else {
            return verifyForgotPasswordOtp(normalizedEmail, request.otpCode());
        }
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
        EmailUtil.validate(request.email());
        String normalizedEmail = EmailUtil.normalize(request.email());
        sendForgotPasswordOtp(normalizedEmail);
    }

    @Override
    @Transactional
    public void resetPassword(@Nullable String rawResetToken, ResetPasswordRequest request) {
        String resetToken = cleanToken(rawResetToken != null && !rawResetToken.isBlank() ? rawResetToken : request.resetToken());
        if (resetToken == null || resetToken.isBlank()) {
            throw new AuthException(AuthErrorCode.AUTH_016, "Password reset token is required");
        }

        if (request.confirmPassword() == null || !request.newPassword().equals(request.confirmPassword())) {
            throw new AuthException(AuthErrorCode.AUTH_017, "Confirm password is different");
        }

        PasswordUtil.validate(request.newPassword());

        String email = resolveEmailFromResetToken(resetToken);
        userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_001));

        updatePasswordAndInvalidateSessions(email, request.newPassword());
        deletePasswordResetToken(resetToken);
    }

    @Override
    @Transactional
    public void changePassword(String userEmail, ChangePasswordRequest request) {
        if (userEmail == null || userEmail.isBlank()) {
            throw new AuthException(AuthErrorCode.AUTH_003, "Unauthenticated user context");
        }
        PasswordUtil.validatePasswordChange(request.currentPassword(), request.newPassword());
        String normalizedEmail = EmailUtil.normalize(userEmail);

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_001));
        verifyCurrentPassword(user, request.currentPassword());
        updatePasswordAndInvalidateSessions(normalizedEmail, request.newPassword());
    }

    // ==========================================
    // Registration Helpers
    // ==========================================

    private UserResponse handleExistingUserRegistration(User existingUser, String newPassword) {
        if (UserStatus.ACTIVE.name().equalsIgnoreCase(existingUser.getStatus())) {
            throw new AuthException(AuthErrorCode.AUTH_001);
        }
        if (UserStatus.BANNED.name().equalsIgnoreCase(existingUser.getStatus())) {
            throw new AuthException(AuthErrorCode.USER_008);
        }
        String encodedPassword = passwordEncoder.encode(newPassword);
        existingUser.setPassword(encodedPassword);
        userRepository.save(existingUser);
        String rawOtp = generateAndStoreOtp(existingUser.getEmail(), OtpType.REGISTER);
        userEventProducer.publishUserRegistered(new UserRegisteredEvent(
                existingUser.getId(),
                existingUser.getEmail(),
                rawOtp,
                Instant.now()
        ));
        return mapToUserResponse(existingUser);
    }

    private UserResponse handleNewUserRegistration(String email, String rawPassword) {
        String encodedPassword = passwordEncoder.encode(rawPassword);
        User newUser = User.createInactive(email, encodedPassword);
        User savedUser = userRepository.save(newUser);
        String rawOtp = generateAndStoreOtp(email, OtpType.REGISTER);
        userEventProducer.publishUserRegistered(new UserRegisteredEvent(
                savedUser.getId(),
                savedUser.getEmail(),
                rawOtp,
                Instant.now()
        ));
        return mapToUserResponse(savedUser);
    }

    private void sendRegisterOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException(AuthErrorCode.AUTH_012));
        validateRegistrationEligibility(user);
        String rawOtp = generateAndStoreOtp(email, OtpType.REGISTER);
        userEventProducer.publishUserRegistered(new UserRegisteredEvent(
                user.getId(),
                user.getEmail(),
                rawOtp,
                Instant.now()
        ));
    }

    private void sendForgotPasswordOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException(AuthErrorCode.AUTH_012));
        validateUserStatus(user);
        String rawOtp = generateAndStoreOtp(email, OtpType.FORGOT_PASSWORD);
        userEventProducer.publishPasswordResetRequested(new PasswordResetRequestedEvent(
                user.getId(),
                user.getEmail(),
                rawOtp,
                Instant.now()
        ));
    }

    // ==========================================
    // OTP Verification Helpers
    // ==========================================

    private AuthResponse verifyRegisterOtp(String email, String otpCode) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException(AuthErrorCode.AUTH_012));
        validateRegistrationEligibility(user);
        verifyOtpInternal(email, otpCode, OtpType.REGISTER);
        user.setStatus(UserStatus.ACTIVE.name());
        userRepository.save(user);
        return generateAuthResponse(user);
    }

    private VerifyOtpResponse verifyForgotPasswordOtp(String email, String otpCode) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException(AuthErrorCode.AUTH_012));
        validateUserStatus(user);
        verifyOtpInternal(email, otpCode, OtpType.FORGOT_PASSWORD);
        String resetToken = createPasswordResetToken(email);
        return new VerifyOtpResponse(resetToken);
    }

    // ==========================================
    // Session & Token Helpers
    // ==========================================

    private AuthResponse generateAuthResponse(User user) {
        String accessToken = jwtUtils.generateAccessToken(user.getEmail(), user.getId().toString(), user.getRole());
        String refreshToken = jwtUtils.generateRefreshToken(user.getEmail());

        saveSession(refreshToken, user.getId(), user.getEmail());
        return new AuthResponse(accessToken, refreshToken, mapToUserResponse(user));
    }

    private AuthResponse rotateUserSessionAndRespond(User user) {
        revokeAllUserSessions(user.getEmail());
        return generateAuthResponse(user);
    }

    private User authenticateUser(User user, String rawPassword) {
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new AuthException(AuthErrorCode.AUTH_003);
        }
        return validateUserStatus(user);
    }

    private User validateUserStatus(User user) {
        if (UserStatus.INACTIVE.name().equalsIgnoreCase(user.getStatus())) {
            throw new AuthException(AuthErrorCode.USER_007);
        }
        if (UserStatus.BANNED.name().equalsIgnoreCase(user.getStatus())) {
            throw new AuthException(AuthErrorCode.USER_008);
        }
        return user;
    }

    private User validateRegistrationEligibility(User user) {
        if (UserStatus.ACTIVE.name().equalsIgnoreCase(user.getStatus())) {
            throw new AuthException(AuthErrorCode.AUTH_015);
        }
        if (UserStatus.BANNED.name().equalsIgnoreCase(user.getStatus())) {
            throw new AuthException(AuthErrorCode.USER_008);
        }
        return user;
    }

    private User resolveUserForTokenRefresh(String refreshToken) {
        String email = jwtUtils.extractEmail(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException(AuthErrorCode.AUTH_003));
        return validateUserStatus(user);
    }

    private void verifyCurrentPassword(User user, String currentPassword) {
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new AuthException(AuthErrorCode.AUTH_013);
        }
    }

    private void updatePasswordAndInvalidateSessions(String email, String newPassword) {
        String encodedPassword = passwordEncoder.encode(newPassword);
        int rows = userRepository.updatePasswordByEmail(email, encodedPassword);
        if (rows <= 0) {
            throw new AuthException(AuthErrorCode.USER_001, "Failed to update user password");
        }
        revokeAllUserSessions(email);
    }

    private UserResponse mapToUserResponse(User user) {
        return userMapper.toUserResponse(user);
    }

    private void validateEmailAndPassword(String email, String password) {
        EmailUtil.validate(email);
        PasswordUtil.validate(password);
    }

    // ==========================================
    // Redis Session Management Logic
    // ==========================================

    private void saveSession(String refreshToken, UUID userId, String email) {
        if (refreshToken == null || userId == null || email == null) {
            throw new IllegalArgumentException("Session parameters must not be null");
        }

        String tokenKey = buildTokenKey(refreshToken);
        String sessionKey = buildUserSessionsKey(email);
        SessionMetadata metadata = new SessionMetadata(userId, email, System.currentTimeMillis());

        try {
            String jsonPayload = objectMapper.writeValueAsString(metadata);
            redisTemplate.opsForValue().set(tokenKey, jsonPayload, refreshTokenTtl);
            redisTemplate.opsForSet().add(sessionKey, tokenKey);
            redisTemplate.expire(sessionKey, refreshTokenTtl);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize session metadata for user [{}]", userId, e);
            throw new RuntimeException("Failed to serialize session metadata", e);
        }
    }

    private boolean isValidSession(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return false;
        }
        Boolean hasKey = redisTemplate.hasKey(buildTokenKey(refreshToken));
        return Boolean.TRUE.equals(hasKey);
    }

    @Nullable
    private SessionMetadata getSession(@Nullable String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return null;
        }
        String json = redisTemplate.opsForValue().get(buildTokenKey(refreshToken));
        if (json == null) {
            return null;
        }
        return deserializeSessionMetadata(json);
    }

    private void revokeSession(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        String tokenKey = buildTokenKey(refreshToken);

        SessionMetadata metadata = getSession(refreshToken);
        if (metadata != null) {
            String sessionKey = buildUserSessionsKey(metadata.email());
            redisTemplate.opsForSet().remove(sessionKey, tokenKey);
        }
        redisTemplate.delete(tokenKey);
    }

    private long revokeAllUserSessions(String email) {
        if (email == null || email.isBlank()) {
            return 0L;
        }
        String sessionKey = buildUserSessionsKey(email);
        List<String> keys = List.of(sessionKey);

        Long count = redisTemplate.execute(revokeAllSessionsScript, keys);
        long result = count != null ? count : 0L;
        log.info("Revoked {} active sessions for user [{}]", result, email);
        return result;
    }

    private SessionMetadata deserializeSessionMetadata(String json) {
        try {
            return objectMapper.readValue(json, SessionMetadata.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize session metadata from Redis: {}", e.getMessage(), e);
            throw new AuthException(AuthErrorCode.AUTH_005, "Corrupted session data format");
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

    private String generateAndStoreOtp(String email, OtpType type) {
        if (email == null || email.isBlank()) {
            throw new AuthException(AuthErrorCode.AUTH_000);
        }
        OtpType resolvedType = type != null ? type : OtpType.FORGOT_PASSWORD;
        String key = buildOtpKey(email, resolvedType);
        OtpData existingData = fetchExistingOtp(key);
        if (existingData != null) {
            checkCooldown(existingData);
        }
        return createAndSaveOtp(key, email, resolvedType);
    }

    private void verifyOtpInternal(String email, String rawOtp, OtpType type) {
        if (email == null || email.isBlank() || rawOtp == null || rawOtp.isBlank()) {
            throw new AuthException(AuthErrorCode.AUTH_009);
        }
        OtpType resolvedType = type != null ? type : OtpType.FORGOT_PASSWORD;
        String key = buildOtpKey(email, resolvedType);
        OtpData otpData = fetchExistingOtp(key);
        if (otpData == null) {
            throw new AuthException(AuthErrorCode.AUTH_008);
        }
        processOtpVerification(key, otpData, rawOtp);
    }

    private String createPasswordResetToken(String email) {
        if (email == null || email.isBlank()) {
            throw new AuthException(AuthErrorCode.AUTH_000);
        }
        String normalizedEmail = email.trim().toLowerCase();
        String resetToken = UUID.randomUUID().toString();
        String key = KEY_PREFIX_RESET_TOKEN + resetToken;

        redisTemplate.opsForValue().set(key, normalizedEmail, RESET_TOKEN_TTL);
        log.info("Generated reset token for user [{}]", normalizedEmail);
        return resetToken;
    }

    private String resolveEmailFromResetToken(String resetToken) {
        if (resetToken == null || resetToken.isBlank()) {
            throw new AuthException(AuthErrorCode.AUTH_016, "Password reset token is required");
        }
        String key = KEY_PREFIX_RESET_TOKEN + resetToken.trim();
        String email = redisTemplate.opsForValue().get(key);
        if (email == null || email.isBlank()) {
            throw new AuthException(AuthErrorCode.AUTH_016, "Invalid or expired password reset token");
        }
        return email;
    }

    private void deletePasswordResetToken(String resetToken) {
        if (resetToken == null || resetToken.isBlank()) {
            return;
        }
        redisTemplate.delete(KEY_PREFIX_RESET_TOKEN + resetToken.trim());
    }

    private void checkCooldown(OtpData existingData) {
        long elapsed = System.currentTimeMillis() - existingData.createdAt();
        if (elapsed < cooldownMillis) {
            throw new AuthException(AuthErrorCode.AUTH_007);
        }
    }

    private String createAndSaveOtp(String key, String email, OtpType type) {
        String rawOtp = generateNumericOtp(otpLength);
        String hashedOtp = passwordEncoder.encode(rawOtp);
        OtpData otpData = new OtpData(hashedOtp, 0, System.currentTimeMillis());

        try {
            String json = objectMapper.writeValueAsString(otpData);
            redisTemplate.opsForValue().set(key, json, otpExpirationDuration);
            log.info("Generated {} OTP for user [{}]", type, email);
            return rawOtp;
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize OTP data for [{}]: {}", email, e.getMessage(), e);
            throw new AuthException(AuthErrorCode.AUTH_011);
        }
    }

    private void processOtpVerification(String key, OtpData otpData, String rawOtp) {
        if (otpData.attempts() >= maxAttempts) {
            redisTemplate.delete(key);
            throw new AuthException(AuthErrorCode.AUTH_010);
        }

        boolean matches = passwordEncoder.matches(rawOtp, otpData.otpCode());
        if (!matches) {
            int newAttempts = otpData.attempts() + 1;
            if (newAttempts >= maxAttempts) {
                redisTemplate.delete(key);
                throw new AuthException(AuthErrorCode.AUTH_010);
            }
            updateAttempts(key, otpData, newAttempts);
            throw new AuthException(AuthErrorCode.AUTH_009,
                    "Invalid OTP code. " + (maxAttempts - newAttempts) + " attempts remaining.");
        }

        redisTemplate.delete(key);
    }

    private void updateAttempts(String key, OtpData otpData, int newAttempts) {
        OtpData updated = new OtpData(otpData.otpCode(), newAttempts, otpData.createdAt());
        try {
            String json = objectMapper.writeValueAsString(updated);
            Long expireSeconds = redisTemplate.getExpire(key);
            Duration safeTtl = (expireSeconds != null && expireSeconds > 0)
                    ? Duration.ofSeconds(expireSeconds)
                    : otpExpirationDuration;
            redisTemplate.opsForValue().set(key, json, safeTtl);
        } catch (JsonProcessingException e) {
            log.error("Failed to update OTP attempt count in Redis: {}", e.getMessage(), e);
            throw new AuthException(AuthErrorCode.AUTH_011, "Failed to update OTP attempts state");
        }
    }

    @Nullable
    private OtpData fetchExistingOtp(String key) {
        String json = redisTemplate.opsForValue().get(key);
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, OtpData.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize OTP data from Redis for key [{}]: {}", key, e.getMessage(), e);
            throw new AuthException(AuthErrorCode.AUTH_011, "Corrupted OTP data format");
        }
    }

    private String generateNumericOtp(int length) {
        return otpUtils.generateNumericOtp(length);
    }

    private String buildOtpKey(String email, OtpType type) {
        String prefix = type == OtpType.REGISTER ? KEY_PREFIX_REGISTER : KEY_PREFIX_FORGOT_PASSWORD;
        return prefix + email.trim().toLowerCase();
    }

    @Nullable
    private String cleanToken(@Nullable String token) {
        if (token == null) {
            return null;
        }
        String cleaned = token.trim();
        while ((cleaned.startsWith("\"") && cleaned.endsWith("\"")) || (cleaned.startsWith("'") && cleaned.endsWith("'"))) {
            cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
        }
        if (cleaned.startsWith("Bearer ")) {
            cleaned = cleaned.substring("Bearer ".length()).trim();
        }
        return cleaned.isBlank() ? null : cleaned;
    }
}
