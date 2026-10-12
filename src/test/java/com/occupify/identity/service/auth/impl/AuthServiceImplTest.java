package com.occupify.identity.service.auth.impl;

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
import com.occupify.identity.enums.UserRole;
import com.occupify.identity.enums.UserStatus;
import com.occupify.identity.exception.auth.AuthErrorCode;
import com.occupify.identity.exception.auth.AuthException;
import com.occupify.identity.repository.UserRepository;
import com.occupify.identity.producer.UserEventProducer;
import com.occupify.identity.security.JwtUtils;
import com.occupify.identity.security.impl.JwtUtilsImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final String TEST_SECRET = "occupify-super-secret-jwt-signing-key-for-unit-testing-must-be-at-least-64-bytes-long!";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SetOperations<String, String> setOperations;

    @Mock
    private RedisScript<Long> revokeAllSessionsScript;

    @Mock
    private UserEventProducer userEventProducer;

    private ObjectMapper objectMapper;
    private JwtUtils jwtUtils;
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        jwtUtils = new JwtUtilsImpl(TEST_SECRET, 3600000, 604800000);
        authService = new AuthServiceImpl(
                userRepository,
                passwordEncoder,
                jwtUtils,
                redisTemplate,
                revokeAllSessionsScript,
                objectMapper,
                userEventProducer,
                6,
                300L,
                3,
                60L,
                604800000L);
    }

    private void mockRedisSessionSave() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        doNothing().when(valueOperations).set(anyString(), anyString(), any(Duration.class));
        when(setOperations.add(anyString(), anyString())).thenReturn(1L);
        when(redisTemplate.expire(anyString(), any(Duration.class))).thenReturn(true);
    }

    private void mockRedisRevokeAllSessions() {
        when(redisTemplate.execute(eq(revokeAllSessionsScript), anyList())).thenReturn(1L);
    }

    private void mockRedisOtpGeneration(String otpKey) {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(otpKey)).thenReturn(null);
        doNothing().when(valueOperations).set(eq(otpKey), anyString(), any(Duration.class));
    }

    private void mockRedisOtpVerification(String otpKey, String rawOtp, String hashedOtp) {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        String json = "{\"otpCode\":\"" + hashedOtp + "\",\"attempts\":0,\"createdAt\":1000}";
        when(valueOperations.get(otpKey)).thenReturn(json);
        when(passwordEncoder.matches(rawOtp, hashedOtp)).thenReturn(true);
        when(redisTemplate.delete(otpKey)).thenReturn(true);
    }

    @Test
    void shouldRegisterNewUserSuccessfully() {
        RegisterRequest request = new RegisterRequest("newuser@occupify.com", "Password123!");
        UUID userId = UUID.randomUUID();
        User savedUser = new User(userId, "newuser@occupify.com", "encoded-pass", UserRole.USER.name(),
                UserStatus.INACTIVE.name(), Instant.now(), Instant.now());

        when(userRepository.findByEmail("newuser@occupify.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Password123!")).thenReturn("encoded-pass");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        mockRedisOtpGeneration("otp:register:newuser@occupify.com");

        UserResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals(userId, response.id());
        assertEquals("newuser@occupify.com", response.email());
        assertEquals("INACTIVE", response.status());
        verify(userEventProducer).publishUserRegistered(
                argThat(event -> event.userId().equals(userId) && event.email().equals("newuser@occupify.com")));
    }

    @Test
    void shouldThrowConflictWhenRegisteringDuplicateActiveEmail() {
        RegisterRequest request = new RegisterRequest("existing@occupify.com", "Password123!");
        User existingUser = new User(UUID.randomUUID(), "existing@occupify.com", "pass", "USER", "ACTIVE",
                Instant.now(), Instant.now());
        when(userRepository.findByEmail("existing@occupify.com")).thenReturn(Optional.of(existingUser));

        AuthException ex = assertThrows(AuthException.class, () -> authService.register(request));
        assertEquals(AuthErrorCode.AUTH_001, ex.getErrorCode());
        verify(userEventProducer, never()).publishUserRegistered(any());
    }

    @Test
    void shouldReRegisterInactiveUserSuccessfully() {
        RegisterRequest request = new RegisterRequest("inactive@occupify.com", "Password123!");
        UUID userId = UUID.randomUUID();
        User inactiveUser = new User(userId, "inactive@occupify.com", "old-pass", "USER", "INACTIVE",
                Instant.now(), Instant.now());

        when(userRepository.findByEmail("inactive@occupify.com")).thenReturn(Optional.of(inactiveUser));
        when(passwordEncoder.encode("Password123!")).thenReturn("new-encoded-pass");
        when(userRepository.save(any(User.class))).thenReturn(inactiveUser);
        mockRedisOtpGeneration("otp:register:inactive@occupify.com");

        UserResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals(userId, response.id());
        assertEquals("INACTIVE", response.status());
        verify(userRepository).save(inactiveUser);
        verify(userEventProducer).publishUserRegistered(
                argThat(event -> event.userId().equals(userId) && event.email().equals("inactive@occupify.com")));
    }

    @Test
    void shouldResendOtpForRegistration() {
        SendOtpRequest request = new SendOtpRequest("inactive@occupify.com", OtpType.REGISTER);
        User user = new User(UUID.randomUUID(), "inactive@occupify.com", "pass", "USER", "INACTIVE",
                Instant.now(), Instant.now());

        when(userRepository.findByEmail("inactive@occupify.com")).thenReturn(Optional.of(user));
        mockRedisOtpGeneration("otp:register:inactive@occupify.com");

        assertDoesNotThrow(() -> authService.resendOtp(request));
        verify(userEventProducer)
                .publishUserRegistered(argThat(event -> event.email().equals("inactive@occupify.com")));
    }

    @Test
    void shouldResendOtpForForgotPassword() {
        SendOtpRequest request = new SendOtpRequest("active@occupify.com", OtpType.FORGOT_PASSWORD);
        User user = new User(UUID.randomUUID(), "active@occupify.com", "pass", "USER", "ACTIVE",
                Instant.now(), Instant.now());

        when(userRepository.findByEmail("active@occupify.com")).thenReturn(Optional.of(user));
        mockRedisOtpGeneration("otp:forgot_password:active@occupify.com");

        assertDoesNotThrow(() -> authService.resendOtp(request));
        verify(userEventProducer)
                .publishPasswordResetRequested(argThat(event -> event.email().equals("active@occupify.com")));
    }

    @Test
    void shouldSendForgotPasswordOtpSuccessfully() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("active@occupify.com");
        User user = new User(UUID.randomUUID(), "active@occupify.com", "pass", "USER", "ACTIVE",
                Instant.now(), Instant.now());

        when(userRepository.findByEmail("active@occupify.com")).thenReturn(Optional.of(user));
        mockRedisOtpGeneration("otp:forgot_password:active@occupify.com");

        assertDoesNotThrow(() -> authService.forgotPassword(request));
        verify(userEventProducer)
                .publishPasswordResetRequested(argThat(event -> event.email().equals("active@occupify.com")));
    }

    @Test
    void shouldVerifyRegisterOtpAndActivateAccount() {
        VerifyOtpRequest request = new VerifyOtpRequest("user@occupify.com", "123456", OtpType.REGISTER);
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "user@occupify.com", "pass", "USER", "INACTIVE",
                Instant.now(), Instant.now());

        when(userRepository.findByEmail("user@occupify.com")).thenReturn(Optional.of(user));
        mockRedisOtpVerification("otp:register:user@occupify.com", "123456", "$2a$10$hashed");
        when(userRepository.save(any(User.class))).thenReturn(user);
        mockRedisSessionSave();

        Object result = authService.verifyOtp(request);

        assertTrue(result instanceof AuthResponse);
        AuthResponse auth = (AuthResponse) result;
        assertNotNull(auth.accessToken());
        assertNotNull(auth.refreshToken());
        assertEquals("ACTIVE", auth.user().status());
        verify(userRepository).save(user);
    }

    @Test
    void shouldVerifyForgotPasswordOtpAndReturnResetToken() {
        VerifyOtpRequest request = new VerifyOtpRequest("user@occupify.com", "123456", OtpType.FORGOT_PASSWORD);
        User user = new User(UUID.randomUUID(), "user@occupify.com", "pass", "USER", "ACTIVE",
                Instant.now(), Instant.now());

        when(userRepository.findByEmail("user@occupify.com")).thenReturn(Optional.of(user));
        mockRedisOtpVerification("otp:forgot_password:user@occupify.com", "123456", "$2a$10$hashed");
        doNothing().when(valueOperations).set(startsWith("password_reset_token:"), eq("user@occupify.com"),
                any(Duration.class));

        Object result = authService.verifyOtp(request);

        assertTrue(result instanceof VerifyOtpResponse);
        VerifyOtpResponse response = (VerifyOtpResponse) result;
        assertNotNull(response.resetToken());
    }

    @Test
    void shouldLoginSuccessfullyWithSessionRotation() {
        LoginRequest request = new LoginRequest("user@occupify.com", "Password123!");
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "user@occupify.com", "encoded-pass", "USER", "ACTIVE",
                Instant.now(), Instant.now());

        when(userRepository.findByEmail("user@occupify.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123!", "encoded-pass")).thenReturn(true);
        mockRedisRevokeAllSessions();
        mockRedisSessionSave();

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertNotNull(response.accessToken());
        assertNotNull(response.refreshToken());
        verify(redisTemplate).execute(eq(revokeAllSessionsScript), eq(List.of("user_sessions:user@occupify.com")));
    }

    @Test
    void shouldThrowUnauthorizedOnBadPassword() {
        LoginRequest request = new LoginRequest("user@occupify.com", "WrongPassword!");
        User user = new User(UUID.randomUUID(), "user@occupify.com", "encoded-pass", "USER", "ACTIVE",
                Instant.now(), Instant.now());

        when(userRepository.findByEmail("user@occupify.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword!", "encoded-pass")).thenReturn(false);

        AuthException ex = assertThrows(AuthException.class, () -> authService.login(request));
        assertEquals(AuthErrorCode.AUTH_003, ex.getErrorCode());
    }

    @Test
    void shouldThrowForbiddenOnInactiveUserLogin() {
        LoginRequest request = new LoginRequest("user@occupify.com", "Password123!");
        User user = new User(UUID.randomUUID(), "user@occupify.com", "encoded-pass", "USER", "INACTIVE",
                Instant.now(), Instant.now());

        when(userRepository.findByEmail("user@occupify.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123!", "encoded-pass")).thenReturn(true);

        AuthException ex = assertThrows(AuthException.class, () -> authService.login(request));
        assertEquals(AuthErrorCode.USER_007, ex.getErrorCode());
    }

    @Test
    void shouldThrowForbiddenOnBannedUserLogin() {
        LoginRequest request = new LoginRequest("user@occupify.com", "Password123!");
        User user = new User(UUID.randomUUID(), "user@occupify.com", "encoded-pass", "USER", "BANNED",
                Instant.now(), Instant.now());

        when(userRepository.findByEmail("user@occupify.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123!", "encoded-pass")).thenReturn(true);

        AuthException ex = assertThrows(AuthException.class, () -> authService.login(request));
        assertEquals(AuthErrorCode.USER_008, ex.getErrorCode());
    }

    @Test
    void shouldRefreshTokenSuccessfully() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "user@occupify.com", "encoded-pass", "USER", "ACTIVE",
                Instant.now(), Instant.now());
        String refreshToken = jwtUtils.generateRefreshToken("user@occupify.com");

        String json = "{\"userId\":\"" + userId + "\",\"email\":\"user@occupify.com\",\"createdAt\":1000}";
        when(redisTemplate.hasKey("refresh_token:" + refreshToken)).thenReturn(true);
        when(userRepository.findByEmail("user@occupify.com")).thenReturn(Optional.of(user));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("refresh_token:" + refreshToken)).thenReturn(json);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.remove(eq("user_sessions:user@occupify.com"), eq("refresh_token:" + refreshToken)))
                .thenReturn(1L);
        when(redisTemplate.delete("refresh_token:" + refreshToken)).thenReturn(true);
        doNothing().when(valueOperations).set(anyString(), anyString(), any(Duration.class));
        when(setOperations.add(anyString(), anyString())).thenReturn(1L);
        when(redisTemplate.expire(anyString(), any(Duration.class))).thenReturn(true);

        AuthResponse response = authService.refreshToken(refreshToken);

        assertNotNull(response.accessToken());
        assertNotNull(response.refreshToken());
    }

    @Test
    void shouldThrowWhenRefreshTokenIsRevokedInRedis() {
        String refreshToken = jwtUtils.generateRefreshToken("user@occupify.com");
        when(redisTemplate.hasKey("refresh_token:" + refreshToken)).thenReturn(false);

        AuthException ex = assertThrows(AuthException.class, () -> authService.refreshToken(refreshToken));
        assertEquals(AuthErrorCode.AUTH_006, ex.getErrorCode());
    }

    @Test
    void shouldThrowAuth005WhenRefreshTokenIsMalformedOrInvalid() {
        String invalidRefreshToken = "malformed.jwt.token";

        AuthException ex = assertThrows(AuthException.class, () -> authService.refreshToken(invalidRefreshToken));
        assertEquals(AuthErrorCode.AUTH_005, ex.getErrorCode());
    }

    @Test
    void shouldThrowAuth006WhenRefreshTokenIsExpired() {
        JwtUtils expiredJwtUtils = new JwtUtilsImpl(TEST_SECRET, 3600000, -1000L);
        String expiredRefreshToken = expiredJwtUtils.generateRefreshToken("user@occupify.com");

        AuthException ex = assertThrows(AuthException.class, () -> authService.refreshToken(expiredRefreshToken));
        assertEquals(AuthErrorCode.AUTH_006, ex.getErrorCode());
    }

    @Test
    void shouldSignOutSuccessfully() {
        String refreshToken = "token-to-signout";
        UUID userId = UUID.randomUUID();
        String json = "{\"userId\":\"" + userId + "\",\"email\":\"user@occupify.com\",\"createdAt\":1000}";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("refresh_token:" + refreshToken)).thenReturn(json);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.remove(eq("user_sessions:user@occupify.com"), eq("refresh_token:" + refreshToken)))
                .thenReturn(1L);
        when(redisTemplate.delete("refresh_token:" + refreshToken)).thenReturn(true);

        assertDoesNotThrow(() -> authService.signOut(refreshToken));
        verify(redisTemplate).delete("refresh_token:" + refreshToken);
    }

    @Test
    void shouldResetPasswordSuccessfullyWithResetToken() {
        ResetPasswordRequest request = new ResetPasswordRequest("NewPassword789!", "NewPassword789!",
                "valid-reset-token");
        User user = new User(UUID.randomUUID(), "user@occupify.com", "old-pass", "USER", "ACTIVE",
                Instant.now(), Instant.now());

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("password_reset_token:valid-reset-token")).thenReturn("user@occupify.com");
        when(userRepository.findByEmail("user@occupify.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("NewPassword789!")).thenReturn("new-encoded-pass");
        when(userRepository.updatePasswordByEmail("user@occupify.com", "new-encoded-pass"))
                .thenReturn(1);
        mockRedisRevokeAllSessions();
        when(redisTemplate.delete("password_reset_token:valid-reset-token")).thenReturn(true);

        assertDoesNotThrow(() -> authService.resetPassword("valid-reset-token", request));

        verify(userRepository).updatePasswordByEmail("user@occupify.com", "new-encoded-pass");
        verify(redisTemplate).execute(eq(revokeAllSessionsScript), eq(List.of("user_sessions:user@occupify.com")));
        verify(redisTemplate).delete("password_reset_token:valid-reset-token");
    }

    @Test
    void shouldFailResetPasswordWhenConfirmPasswordIsDifferent() {
        ResetPasswordRequest request = new ResetPasswordRequest("NewPassword789!", "DifferentPassword123!",
                "valid-reset-token");

        AuthException ex = assertThrows(AuthException.class,
                () -> authService.resetPassword("valid-reset-token", request));
        assertEquals(AuthErrorCode.AUTH_017, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Confirm password is different"));
    }

    @Test
    void shouldFailResetPasswordWhenResetTokenIsMissing() {
        ResetPasswordRequest request = new ResetPasswordRequest("NewPassword789!", "NewPassword789!", null);

        AuthException ex = assertThrows(AuthException.class, () -> authService.resetPassword("", request));
        assertEquals(AuthErrorCode.AUTH_016, ex.getErrorCode());
    }

    @Test
    void shouldFailResetPasswordWhenResetTokenIsInvalidOrExpired() {
        ResetPasswordRequest request = new ResetPasswordRequest("NewPassword789!", "NewPassword789!", "expired-token");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("password_reset_token:expired-token")).thenReturn(null);

        AuthException ex = assertThrows(AuthException.class, () -> authService.resetPassword("expired-token", request));
        assertEquals(AuthErrorCode.AUTH_016, ex.getErrorCode());
    }

    @Test
    void shouldChangePasswordWithVerification() {
        ChangePasswordRequest request = new ChangePasswordRequest("CurrentPass123!", "NewPass456789!");
        User user = new User(UUID.randomUUID(), "user@occupify.com", "hashed-current-pass", "USER", "ACTIVE",
                Instant.now(), Instant.now());

        when(userRepository.findByEmail("user@occupify.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("CurrentPass123!", "hashed-current-pass")).thenReturn(true);
        when(passwordEncoder.encode("NewPass456789!")).thenReturn("hashed-new-pass");
        when(userRepository.updatePasswordByEmail("user@occupify.com", "hashed-new-pass"))
                .thenReturn(1);
        mockRedisRevokeAllSessions();

        assertDoesNotThrow(() -> authService.changePassword("user@occupify.com", request));

        verify(redisTemplate).execute(eq(revokeAllSessionsScript), eq(List.of("user_sessions:user@occupify.com")));
    }

    @Test
    void shouldThrowWhenCurrentPasswordIsIncorrectOnPasswordChange() {
        ChangePasswordRequest request = new ChangePasswordRequest("WrongPass!", "NewPass456789!");
        User user = new User(UUID.randomUUID(), "user@occupify.com", "hashed-current-pass", "USER", "ACTIVE",
                Instant.now(), Instant.now());

        when(userRepository.findByEmail("user@occupify.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPass!", "hashed-current-pass")).thenReturn(false);

        AuthException ex = assertThrows(AuthException.class,
                () -> authService.changePassword("user@occupify.com", request));
        assertEquals(AuthErrorCode.AUTH_013, ex.getErrorCode());
    }

    @Test
    void shouldThrowWhenPasswordUpdateAffectsZeroRowsOnChangePassword() {
        ChangePasswordRequest request = new ChangePasswordRequest("CurrentPass123!", "NewPass456789!");
        User user = new User(UUID.randomUUID(), "user@occupify.com", "hashed-current-pass", "USER", "ACTIVE",
                Instant.now(), Instant.now());

        when(userRepository.findByEmail("user@occupify.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("CurrentPass123!", "hashed-current-pass")).thenReturn(true);
        when(passwordEncoder.encode("NewPass456789!")).thenReturn("hashed-new-pass");
        when(userRepository.updatePasswordByEmail("user@occupify.com", "hashed-new-pass"))
                .thenReturn(0);

        AuthException ex = assertThrows(AuthException.class,
                () -> authService.changePassword("user@occupify.com", request));
        assertEquals(AuthErrorCode.USER_001, ex.getErrorCode());
        verify(redisTemplate, never()).execute(eq(revokeAllSessionsScript), anyList());
    }
}
