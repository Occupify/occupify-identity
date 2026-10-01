package com.occupify.identity.service;

import com.occupify.identity.dto.request.ChangePasswordRequest;
import com.occupify.identity.dto.request.ForgotPasswordRequest;
import com.occupify.identity.dto.request.LoginRequest;
import com.occupify.identity.dto.request.RegisterRequest;
import com.occupify.identity.dto.request.ResetPasswordRequest;
import com.occupify.identity.entity.User;
import com.occupify.identity.enums.UserRole;
import com.occupify.identity.enums.UserStatus;
import com.occupify.identity.exception.AuthErrorCode;
import com.occupify.identity.exception.AuthException;
import com.occupify.identity.jwt.JwtUtils;
import com.occupify.identity.repository.UserRepository;
import com.occupify.identity.service.client.NotificationClient;
import com.occupify.identity.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
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
    private SessionService sessionService;

    @Mock
    private OtpService otpService;

    @Mock
    private NotificationClient notificationClient;

    private JwtUtils jwtUtils;
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils(TEST_SECRET, 3600000, 604800000);
        authService = new AuthServiceImpl(
                userRepository,
                passwordEncoder,
                jwtUtils,
                sessionService,
                otpService,
                notificationClient
        );
    }

    @Test
    void shouldRegisterNewUserSuccessfully() {
        RegisterRequest request = new RegisterRequest("newuser@occupify.com", "Password123!");
        UUID userId = UUID.randomUUID();
        User savedUser = new User(userId, "newuser@occupify.com", "encoded-pass", UserRole.USER.name(), UserStatus.ACTIVE.name(), Instant.now(), Instant.now());

        when(userRepository.existsByEmail("newuser@occupify.com")).thenReturn(Mono.just(false));
        when(passwordEncoder.encode("Password123!")).thenReturn("encoded-pass");
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(savedUser));
        when(sessionService.saveSession(anyString(), eq(userId), eq("newuser@occupify.com"))).thenReturn(Mono.empty());

        StepVerifier.create(authService.register(request))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertNotNull(response.accessToken());
                    assertNotNull(response.refreshToken());
                    assertEquals("newuser@occupify.com", response.user().email());
                })
                .verifyComplete();
    }

    @Test
    void shouldThrowConflictWhenRegisteringDuplicateEmail() {
        RegisterRequest request = new RegisterRequest("existing@occupify.com", "Password123!");
        when(userRepository.existsByEmail("existing@occupify.com")).thenReturn(Mono.just(true));

        StepVerifier.create(authService.register(request))
                .expectErrorMatches(ex -> ex instanceof AuthException ae && ae.getErrorCode() == AuthErrorCode.AUTH_001)
                .verify();
    }

    @Test
    void shouldLoginSuccessfullyWithSessionRotation() {
        LoginRequest request = new LoginRequest("user@occupify.com", "Password123!");
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "user@occupify.com", "encoded-pass", "USER", "ACTIVE", Instant.now(), Instant.now());

        when(userRepository.findByEmail("user@occupify.com")).thenReturn(Mono.just(user));
        when(passwordEncoder.matches("Password123!", "encoded-pass")).thenReturn(true);
        when(sessionService.revokeAllUserSessions("user@occupify.com")).thenReturn(Mono.just(1L));
        when(sessionService.saveSession(anyString(), eq(userId), eq("user@occupify.com"))).thenReturn(Mono.empty());

        StepVerifier.create(authService.login(request))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertNotNull(response.accessToken());
                    assertNotNull(response.refreshToken());
                })
                .verifyComplete();

        verify(sessionService).revokeAllUserSessions("user@occupify.com");
    }

    @Test
    void shouldThrowUnauthorizedOnBadPassword() {
        LoginRequest request = new LoginRequest("user@occupify.com", "WrongPassword!");
        User user = new User(UUID.randomUUID(), "user@occupify.com", "encoded-pass", "USER", "ACTIVE", Instant.now(), Instant.now());

        when(userRepository.findByEmail("user@occupify.com")).thenReturn(Mono.just(user));
        when(passwordEncoder.matches("WrongPassword!", "encoded-pass")).thenReturn(false);

        StepVerifier.create(authService.login(request))
                .expectErrorMatches(ex -> ex instanceof AuthException ae && ae.getErrorCode() == AuthErrorCode.AUTH_003)
                .verify();
    }

    @Test
    void shouldThrowForbiddenOnInactiveUserLogin() {
        LoginRequest request = new LoginRequest("user@occupify.com", "Password123!");
        User user = new User(UUID.randomUUID(), "user@occupify.com", "encoded-pass", "USER", "INACTIVE", Instant.now(), Instant.now());

        when(userRepository.findByEmail("user@occupify.com")).thenReturn(Mono.just(user));
        when(passwordEncoder.matches("Password123!", "encoded-pass")).thenReturn(true);

        StepVerifier.create(authService.login(request))
                .expectErrorMatches(ex -> ex instanceof AuthException ae && ae.getErrorCode() == AuthErrorCode.USER_007)
                .verify();
    }

    @Test
    void shouldThrowForbiddenOnBannedUserLogin() {
        LoginRequest request = new LoginRequest("user@occupify.com", "Password123!");
        User user = new User(UUID.randomUUID(), "user@occupify.com", "encoded-pass", "USER", "BANNED", Instant.now(), Instant.now());

        when(userRepository.findByEmail("user@occupify.com")).thenReturn(Mono.just(user));
        when(passwordEncoder.matches("Password123!", "encoded-pass")).thenReturn(true);

        StepVerifier.create(authService.login(request))
                .expectErrorMatches(ex -> ex instanceof AuthException ae && ae.getErrorCode() == AuthErrorCode.USER_008)
                .verify();
    }

    @Test
    void shouldRefreshTokenSuccessfully() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "user@occupify.com", "encoded-pass", "USER", "ACTIVE", Instant.now(), Instant.now());
        String refreshToken = jwtUtils.generateRefreshToken("user@occupify.com");

        when(sessionService.isValidSession(refreshToken)).thenReturn(Mono.just(true));
        when(userRepository.findByEmail("user@occupify.com")).thenReturn(Mono.just(user));

        StepVerifier.create(authService.refreshToken(refreshToken))
                .assertNext(response -> {
                    assertNotNull(response.accessToken());
                    assertEquals(refreshToken, response.refreshToken());
                })
                .verifyComplete();
    }

    @Test
    void shouldThrowWhenRefreshTokenIsRevokedInRedis() {
        String refreshToken = jwtUtils.generateRefreshToken("user@occupify.com");
        when(sessionService.isValidSession(refreshToken)).thenReturn(Mono.just(false));

        StepVerifier.create(authService.refreshToken(refreshToken))
                .expectErrorMatches(ex -> ex instanceof AuthException ae && ae.getErrorCode() == AuthErrorCode.AUTH_006)
                .verify();
    }

    @Test
    void shouldSignOutSuccessfully() {
        when(sessionService.revokeSession("token-to-signout")).thenReturn(Mono.empty());

        StepVerifier.create(authService.signOut("token-to-signout"))
                .verifyComplete();

        verify(sessionService).revokeSession("token-to-signout");
    }

    @Test
    void shouldDispatchForgotPasswordOtp() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("user@occupify.com");
        when(userRepository.existsByEmail("user@occupify.com")).thenReturn(Mono.just(true));
        when(otpService.generateAndStoreOtp("user@occupify.com")).thenReturn(Mono.just("123456"));
        when(notificationClient.sendPasswordResetOtp("user@occupify.com", "123456")).thenReturn(Mono.empty());

        StepVerifier.create(authService.forgotPassword(request))
                .verifyComplete();

        verify(notificationClient).sendPasswordResetOtp("user@occupify.com", "123456");
    }

    @Test
    void shouldFailForgotPasswordWhenNotificationDispatchFails() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("user@occupify.com");
        when(userRepository.existsByEmail("user@occupify.com")).thenReturn(Mono.just(true));
        when(otpService.generateAndStoreOtp("user@occupify.com")).thenReturn(Mono.just("123456"));
        when(notificationClient.sendPasswordResetOtp("user@occupify.com", "123456"))
                .thenReturn(Mono.error(new AuthException(AuthErrorCode.AUTH_011)));

        StepVerifier.create(authService.forgotPassword(request))
                .expectErrorMatches(throwable -> throwable instanceof AuthException authEx
                        && authEx.getErrorCode() == AuthErrorCode.AUTH_011)
                .verify();

        verify(notificationClient).sendPasswordResetOtp("user@occupify.com", "123456");
    }

    @Test
    void shouldResetPasswordAndRevokeAllSessions() {
        ResetPasswordRequest request = new ResetPasswordRequest("user@occupify.com", "123456", "NewPassword789!");
        User user = new User(UUID.randomUUID(), "user@occupify.com", "old-pass", "USER", "ACTIVE", Instant.now(), Instant.now());

        when(otpService.verifyOtp("user@occupify.com", "123456")).thenReturn(Mono.empty());
        when(userRepository.findByEmail("user@occupify.com")).thenReturn(Mono.just(user));
        when(passwordEncoder.encode("NewPassword789!")).thenReturn("new-encoded-pass");
        when(userRepository.updatePasswordByEmail("user@occupify.com", "new-encoded-pass")).thenReturn(Mono.just(1));
        when(sessionService.revokeAllUserSessions("user@occupify.com")).thenReturn(Mono.just(2L));

        StepVerifier.create(authService.resetPassword(request))
                .verifyComplete();

        verify(userRepository).updatePasswordByEmail("user@occupify.com", "new-encoded-pass");
        verify(sessionService).revokeAllUserSessions("user@occupify.com");
    }

    @Test
    void shouldChangePasswordWithVerification() {
        ChangePasswordRequest request = new ChangePasswordRequest("CurrentPass123!", "NewPass456789!");
        User user = new User(UUID.randomUUID(), "user@occupify.com", "hashed-current-pass", "USER", "ACTIVE", Instant.now(), Instant.now());

        when(userRepository.findByEmail("user@occupify.com")).thenReturn(Mono.just(user));
        when(passwordEncoder.matches("CurrentPass123!", "hashed-current-pass")).thenReturn(true);
        when(passwordEncoder.encode("NewPass456789!")).thenReturn("hashed-new-pass");
        when(userRepository.updatePasswordByEmail("user@occupify.com", "hashed-new-pass")).thenReturn(Mono.just(1));
        when(sessionService.revokeAllUserSessions("user@occupify.com")).thenReturn(Mono.just(1L));

        StepVerifier.create(authService.changePassword("user@occupify.com", request))
                .verifyComplete();

        verify(sessionService).revokeAllUserSessions("user@occupify.com");
    }

    @Test
    void shouldThrowWhenCurrentPasswordIsIncorrectOnPasswordChange() {
        ChangePasswordRequest request = new ChangePasswordRequest("WrongPass!", "NewPass456789!");
        User user = new User(UUID.randomUUID(), "user@occupify.com", "hashed-current-pass", "USER", "ACTIVE", Instant.now(), Instant.now());

        when(userRepository.findByEmail("user@occupify.com")).thenReturn(Mono.just(user));
        when(passwordEncoder.matches("WrongPass!", "hashed-current-pass")).thenReturn(false);

        StepVerifier.create(authService.changePassword("user@occupify.com", request))
                .expectErrorMatches(ex -> ex instanceof AuthException ae && ae.getErrorCode() == AuthErrorCode.AUTH_013)
                .verify();
    }
}
