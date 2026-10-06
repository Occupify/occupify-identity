package com.occupify.identity.service.auth.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.occupify.identity.dto.request.auth.ChangePasswordRequest;
import com.occupify.identity.dto.request.auth.LoginRequest;
import com.occupify.identity.dto.request.auth.RegisterRequest;
import com.occupify.identity.dto.request.auth.ResetPasswordRequest;
import com.occupify.identity.dto.request.auth.SendOtpRequest;
import com.occupify.identity.dto.request.auth.VerifyOtpRequest;
import com.occupify.identity.dto.response.auth.AuthResponse;
import com.occupify.identity.dto.response.auth.VerifyOtpResponse;
import com.occupify.identity.entity.User;
import com.occupify.identity.enums.OtpType;
import com.occupify.identity.enums.UserRole;
import com.occupify.identity.enums.UserStatus;
import com.occupify.identity.exception.auth.AuthErrorCode;
import com.occupify.identity.exception.auth.AuthException;
import com.occupify.identity.repository.UserRepository;
import com.occupify.identity.security.JwtUtils;
import com.occupify.identity.security.impl.JwtUtilsImpl;
import com.occupify.identity.service.email.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveSetOperations;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
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
	private EmailService emailService;

	@Mock
	private ReactiveRedisOperations<String, String> redisTemplate;

	@Mock
	private ReactiveValueOperations<String, String> valueOperations;

	@Mock
	private ReactiveSetOperations<String, String> setOperations;

	@Mock
	private RedisScript<Long> revokeAllSessionsScript;

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
				emailService,
				redisTemplate,
				revokeAllSessionsScript,
				objectMapper,
				6,
				300L,
				3,
				60L,
				604800000L);
	}

	private void mockRedisSessionSave() {
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(redisTemplate.opsForSet()).thenReturn(setOperations);
		when(valueOperations.set(anyString(), anyString(), any(Duration.class))).thenReturn(Mono.just(true));
		when(setOperations.add(anyString(), anyString())).thenReturn(Mono.just(1L));
		when(redisTemplate.expire(anyString(), any(Duration.class))).thenReturn(Mono.just(true));
	}

	private void mockRedisRevokeAllSessions() {
		when(redisTemplate.execute(eq(revokeAllSessionsScript), anyList())).thenReturn(Flux.just(1L));
	}

	private void mockRedisOtpGeneration(String otpKey) {
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get(otpKey)).thenReturn(Mono.empty());
		when(valueOperations.set(eq(otpKey), anyString(), any(Duration.class))).thenReturn(Mono.just(true));
	}

	private void mockRedisOtpVerification(String otpKey, String rawOtp, String hashedOtp) {
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		String json = "{\"otpCode\":\"" + hashedOtp + "\",\"attempts\":0,\"createdAt\":1000}";
		when(valueOperations.get(otpKey)).thenReturn(Mono.just(json));
		when(passwordEncoder.matches(rawOtp, hashedOtp)).thenReturn(true);
		when(redisTemplate.delete(otpKey)).thenReturn(Mono.just(1L));
	}

	@Test
	void shouldRegisterNewUserSuccessfully() {
		RegisterRequest request = new RegisterRequest("newuser@occupify.com", "Password123!");
		UUID userId = UUID.randomUUID();
		User savedUser = new User(userId, "newuser@occupify.com", "encoded-pass", UserRole.USER.name(),
				UserStatus.INACTIVE.name(), Instant.now(), Instant.now());

		when(userRepository.findByEmail("newuser@occupify.com")).thenReturn(Mono.empty());
		when(passwordEncoder.encode("Password123!")).thenReturn("encoded-pass");
		when(userRepository.save(any(User.class))).thenReturn(Mono.just(savedUser));
		mockRedisOtpGeneration("otp:register:newuser@occupify.com");
		when(emailService.sendRegistrationOtp(eq("newuser@occupify.com"), anyString())).thenReturn(Mono.empty());

		StepVerifier.create(authService.register(request))
				.assertNext(response -> {
					assertNotNull(response);
					assertEquals(userId, response.id());
					assertEquals("newuser@occupify.com", response.email());
					assertEquals("INACTIVE", response.status());
				})
				.verifyComplete();

		verify(emailService).sendRegistrationOtp(eq("newuser@occupify.com"), anyString());
	}

	@Test
	void shouldThrowConflictWhenRegisteringDuplicateActiveEmail() {
		RegisterRequest request = new RegisterRequest("existing@occupify.com", "Password123!");
		User existingUser = new User(UUID.randomUUID(), "existing@occupify.com", "pass", "USER", "ACTIVE",
				Instant.now(), Instant.now());
		when(userRepository.findByEmail("existing@occupify.com")).thenReturn(Mono.just(existingUser));

		StepVerifier.create(authService.register(request))
				.expectErrorMatches(ex -> ex instanceof AuthException ae
						&& ae.getErrorCode() == AuthErrorCode.AUTH_001)
				.verify();
	}

	@Test
	void shouldReRegisterInactiveUserSuccessfully() {
		RegisterRequest request = new RegisterRequest("inactive@occupify.com", "Password123!");
		UUID userId = UUID.randomUUID();
		User inactiveUser = new User(userId, "inactive@occupify.com", "old-pass", "USER", "INACTIVE",
				Instant.now(), Instant.now());

		when(userRepository.findByEmail("inactive@occupify.com")).thenReturn(Mono.just(inactiveUser));
		when(passwordEncoder.encode("Password123!")).thenReturn("new-encoded-pass");
		when(userRepository.updatePasswordByEmail("inactive@occupify.com", "new-encoded-pass"))
				.thenReturn(Mono.just(1));
		mockRedisOtpGeneration("otp:register:inactive@occupify.com");
		when(emailService.sendRegistrationOtp(eq("inactive@occupify.com"), anyString())).thenReturn(Mono.empty());

		StepVerifier.create(authService.register(request))
				.assertNext(response -> {
					assertNotNull(response);
					assertEquals(userId, response.id());
					assertEquals("INACTIVE", response.status());
				})
				.verifyComplete();

		verify(userRepository).updatePasswordByEmail("inactive@occupify.com", "new-encoded-pass");
		verify(emailService).sendRegistrationOtp(eq("inactive@occupify.com"), anyString());
	}

	@Test
	void shouldResendOtpForRegistration() {
		SendOtpRequest request = new SendOtpRequest("inactive@occupify.com", OtpType.REGISTER);
		User user = new User(UUID.randomUUID(), "inactive@occupify.com", "pass", "USER", "INACTIVE",
				Instant.now(), Instant.now());

		when(userRepository.findByEmail("inactive@occupify.com")).thenReturn(Mono.just(user));
		mockRedisOtpGeneration("otp:register:inactive@occupify.com");
		when(emailService.sendRegistrationOtp(eq("inactive@occupify.com"), anyString())).thenReturn(Mono.empty());

		StepVerifier.create(authService.resendOtp(request))
				.verifyComplete();

		verify(emailService).sendRegistrationOtp(eq("inactive@occupify.com"), anyString());
	}

	@Test
	void shouldResendOtpForForgotPassword() {
		SendOtpRequest request = new SendOtpRequest("active@occupify.com", OtpType.FORGOT_PASSWORD);
		User user = new User(UUID.randomUUID(), "active@occupify.com", "pass", "USER", "ACTIVE",
				Instant.now(), Instant.now());

		when(userRepository.findByEmail("active@occupify.com")).thenReturn(Mono.just(user));
		mockRedisOtpGeneration("otp:forgot_password:active@occupify.com");
		when(emailService.sendPasswordResetOtp(eq("active@occupify.com"), anyString())).thenReturn(Mono.empty());

		StepVerifier.create(authService.resendOtp(request))
				.verifyComplete();

		verify(emailService).sendPasswordResetOtp(eq("active@occupify.com"), anyString());
	}

	@Test
	void shouldVerifyRegisterOtpAndActivateAccount() {
		VerifyOtpRequest request = new VerifyOtpRequest("user@occupify.com", "123456", OtpType.REGISTER);
		UUID userId = UUID.randomUUID();
		User user = new User(userId, "user@occupify.com", "pass", "USER", "INACTIVE",
				Instant.now(), Instant.now());

		when(userRepository.findByEmail("user@occupify.com")).thenReturn(Mono.just(user));
		mockRedisOtpVerification("otp:register:user@occupify.com", "123456", "$2a$10$hashed");
		when(userRepository.updateStatusByEmail("user@occupify.com", "ACTIVE")).thenReturn(Mono.just(1));
		mockRedisSessionSave();

		StepVerifier.create(authService.verifyOtp(request))
				.assertNext(result -> {
					assertTrue(result instanceof AuthResponse);
					AuthResponse auth = (AuthResponse) result;
					assertNotNull(auth.accessToken());
					assertNotNull(auth.refreshToken());
					assertEquals("ACTIVE", auth.user().status());
				})
				.verifyComplete();

		verify(userRepository).updateStatusByEmail("user@occupify.com", "ACTIVE");
	}

	@Test
	void shouldVerifyForgotPasswordOtpAndReturnResetToken() {
		VerifyOtpRequest request = new VerifyOtpRequest("user@occupify.com", "123456", OtpType.FORGOT_PASSWORD);
		User user = new User(UUID.randomUUID(), "user@occupify.com", "pass", "USER", "ACTIVE",
				Instant.now(), Instant.now());

		when(userRepository.findByEmail("user@occupify.com")).thenReturn(Mono.just(user));
		mockRedisOtpVerification("otp:forgot_password:user@occupify.com", "123456", "$2a$10$hashed");
		when(valueOperations.set(startsWith("password_reset_token:"), eq("user@occupify.com"), any(Duration.class)))
				.thenReturn(Mono.just(true));

		StepVerifier.create(authService.verifyOtp(request))
				.assertNext(result -> {
					assertTrue(result instanceof VerifyOtpResponse);
					VerifyOtpResponse response = (VerifyOtpResponse) result;
					assertNotNull(response.resetToken());
				})
				.verifyComplete();
	}

	@Test
	void shouldLoginSuccessfullyWithSessionRotation() {
		LoginRequest request = new LoginRequest("user@occupify.com", "Password123!");
		UUID userId = UUID.randomUUID();
		User user = new User(userId, "user@occupify.com", "encoded-pass", "USER", "ACTIVE",
				Instant.now(), Instant.now());

		when(userRepository.findByEmail("user@occupify.com")).thenReturn(Mono.just(user));
		when(passwordEncoder.matches("Password123!", "encoded-pass")).thenReturn(true);
		mockRedisRevokeAllSessions();
		mockRedisSessionSave();

		StepVerifier.create(authService.login(request))
				.assertNext(response -> {
					assertNotNull(response);
					assertNotNull(response.accessToken());
					assertNotNull(response.refreshToken());
				})
				.verifyComplete();

		verify(redisTemplate).execute(eq(revokeAllSessionsScript), eq(List.of("user_sessions:user@occupify.com")));
	}

	@Test
	void shouldThrowUnauthorizedOnBadPassword() {
		LoginRequest request = new LoginRequest("user@occupify.com", "WrongPassword!");
		User user = new User(UUID.randomUUID(), "user@occupify.com", "encoded-pass", "USER", "ACTIVE",
				Instant.now(), Instant.now());

		when(userRepository.findByEmail("user@occupify.com")).thenReturn(Mono.just(user));
		when(passwordEncoder.matches("WrongPassword!", "encoded-pass")).thenReturn(false);

		StepVerifier.create(authService.login(request))
				.expectErrorMatches(ex -> ex instanceof AuthException ae
						&& ae.getErrorCode() == AuthErrorCode.AUTH_003)
				.verify();
	}

	@Test
	void shouldThrowForbiddenOnInactiveUserLogin() {
		LoginRequest request = new LoginRequest("user@occupify.com", "Password123!");
		User user = new User(UUID.randomUUID(), "user@occupify.com", "encoded-pass", "USER", "INACTIVE",
				Instant.now(), Instant.now());

		when(userRepository.findByEmail("user@occupify.com")).thenReturn(Mono.just(user));
		when(passwordEncoder.matches("Password123!", "encoded-pass")).thenReturn(true);

		StepVerifier.create(authService.login(request))
				.expectErrorMatches(ex -> ex instanceof AuthException ae
						&& ae.getErrorCode() == AuthErrorCode.USER_007)
				.verify();
	}

	@Test
	void shouldThrowForbiddenOnBannedUserLogin() {
		LoginRequest request = new LoginRequest("user@occupify.com", "Password123!");
		User user = new User(UUID.randomUUID(), "user@occupify.com", "encoded-pass", "USER", "BANNED",
				Instant.now(), Instant.now());

		when(userRepository.findByEmail("user@occupify.com")).thenReturn(Mono.just(user));
		when(passwordEncoder.matches("Password123!", "encoded-pass")).thenReturn(true);

		StepVerifier.create(authService.login(request))
				.expectErrorMatches(ex -> ex instanceof AuthException ae
						&& ae.getErrorCode() == AuthErrorCode.USER_008)
				.verify();
	}

	@Test
	void shouldRefreshTokenSuccessfully() {
		UUID userId = UUID.randomUUID();
		User user = new User(userId, "user@occupify.com", "encoded-pass", "USER", "ACTIVE",
				Instant.now(), Instant.now());
		String refreshToken = jwtUtils.generateRefreshToken("user@occupify.com");

		when(redisTemplate.hasKey("refresh_token:" + refreshToken)).thenReturn(Mono.just(true));
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
		when(redisTemplate.hasKey("refresh_token:" + refreshToken)).thenReturn(Mono.just(false));

		StepVerifier.create(authService.refreshToken(refreshToken))
				.expectErrorMatches(ex -> ex instanceof AuthException ae
						&& ae.getErrorCode() == AuthErrorCode.AUTH_006)
				.verify();
	}

	@Test
	void shouldSignOutSuccessfully() {
		String refreshToken = "token-to-signout";
		UUID userId = UUID.randomUUID();
		String json = "{\"userId\":\"" + userId + "\",\"email\":\"user@occupify.com\",\"createdAt\":1000}";

		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get("refresh_token:" + refreshToken)).thenReturn(Mono.just(json));
		when(redisTemplate.opsForSet()).thenReturn(setOperations);
		when(setOperations.remove(eq("user_sessions:user@occupify.com"), eq("refresh_token:" + refreshToken)))
				.thenReturn(Mono.just(1L));
		when(redisTemplate.delete("refresh_token:" + refreshToken)).thenReturn(Mono.just(1L));

		StepVerifier.create(authService.signOut(refreshToken))
				.verifyComplete();

		verify(redisTemplate).delete("refresh_token:" + refreshToken);
	}

	@Test
	void shouldResetPasswordWithTokenAndRevokeAllSessions() {
		ResetPasswordRequest request = new ResetPasswordRequest("user@occupify.com", "reset-token", null,
				"NewPassword789!");
		User user = new User(UUID.randomUUID(), "user@occupify.com", "old-pass", "USER", "ACTIVE",
				Instant.now(), Instant.now());

		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get("password_reset_token:reset-token")).thenReturn(Mono.just("user@occupify.com"));
		when(userRepository.findByEmail("user@occupify.com")).thenReturn(Mono.just(user));
		when(passwordEncoder.encode("NewPassword789!")).thenReturn("new-encoded-pass");
		when(userRepository.updatePasswordByEmail("user@occupify.com", "new-encoded-pass"))
				.thenReturn(Mono.just(1));
		mockRedisRevokeAllSessions();
		when(redisTemplate.delete("password_reset_token:reset-token")).thenReturn(Mono.just(1L));

		StepVerifier.create(authService.resetPassword(request))
				.verifyComplete();

		verify(userRepository).updatePasswordByEmail("user@occupify.com", "new-encoded-pass");
		verify(redisTemplate).execute(eq(revokeAllSessionsScript), eq(List.of("user_sessions:user@occupify.com")));
		verify(redisTemplate).delete("password_reset_token:reset-token");
	}

	@Test
	void shouldResetPasswordWithOtpSuccessfully() {
		ResetPasswordRequest request = new ResetPasswordRequest("user@occupify.com", null, "123456",
				"NewPassword789!");
		User user = new User(UUID.randomUUID(), "user@occupify.com", "old-pass", "USER", "ACTIVE",
				Instant.now(), Instant.now());

		mockRedisOtpVerification("otp:forgot_password:user@occupify.com", "123456", "$2a$10$hashed");
		when(userRepository.findByEmail("user@occupify.com")).thenReturn(Mono.just(user));
		when(passwordEncoder.encode("NewPassword789!")).thenReturn("new-encoded-pass");
		when(userRepository.updatePasswordByEmail("user@occupify.com", "new-encoded-pass"))
				.thenReturn(Mono.just(1));
		mockRedisRevokeAllSessions();

		StepVerifier.create(authService.resetPassword(request))
				.verifyComplete();

		verify(userRepository).updatePasswordByEmail("user@occupify.com", "new-encoded-pass");
		verify(redisTemplate).execute(eq(revokeAllSessionsScript), eq(List.of("user_sessions:user@occupify.com")));
	}

	@Test
	void shouldChangePasswordWithVerification() {
		ChangePasswordRequest request = new ChangePasswordRequest("CurrentPass123!", "NewPass456789!");
		User user = new User(UUID.randomUUID(), "user@occupify.com", "hashed-current-pass", "USER", "ACTIVE",
				Instant.now(), Instant.now());

		when(userRepository.findByEmail("user@occupify.com")).thenReturn(Mono.just(user));
		when(passwordEncoder.matches("CurrentPass123!", "hashed-current-pass")).thenReturn(true);
		when(passwordEncoder.encode("NewPass456789!")).thenReturn("hashed-new-pass");
		when(userRepository.updatePasswordByEmail("user@occupify.com", "hashed-new-pass"))
				.thenReturn(Mono.just(1));
		mockRedisRevokeAllSessions();

		StepVerifier.create(authService.changePassword("user@occupify.com", request))
				.verifyComplete();

		verify(redisTemplate).execute(eq(revokeAllSessionsScript), eq(List.of("user_sessions:user@occupify.com")));
	}

	@Test
	void shouldThrowWhenCurrentPasswordIsIncorrectOnPasswordChange() {
		ChangePasswordRequest request = new ChangePasswordRequest("WrongPass!", "NewPass456789!");
		User user = new User(UUID.randomUUID(), "user@occupify.com", "hashed-current-pass", "USER", "ACTIVE",
				Instant.now(), Instant.now());

		when(userRepository.findByEmail("user@occupify.com")).thenReturn(Mono.just(user));
		when(passwordEncoder.matches("WrongPass!", "hashed-current-pass")).thenReturn(false);

		StepVerifier.create(authService.changePassword("user@occupify.com", request))
				.expectErrorMatches(ex -> ex instanceof AuthException ae
						&& ae.getErrorCode() == AuthErrorCode.AUTH_013)
				.verify();
	}

	@Test
	void shouldThrowWhenUpdateStatusAffectsZeroRowsDuringRegistrationOtpVerification() {
		VerifyOtpRequest request = new VerifyOtpRequest("user@occupify.com", "123456", OtpType.REGISTER);
		UUID userId = UUID.randomUUID();
		User user = new User(userId, "user@occupify.com", "pass", "USER", "INACTIVE",
				Instant.now(), Instant.now());

		when(userRepository.findByEmail("user@occupify.com")).thenReturn(Mono.just(user));
		mockRedisOtpVerification("otp:register:user@occupify.com", "123456", "$2a$10$hashed");
		when(userRepository.updateStatusByEmail("user@occupify.com", "ACTIVE")).thenReturn(Mono.just(0));

		StepVerifier.create(authService.verifyOtp(request))
				.expectErrorMatches(ex -> ex instanceof AuthException ae
						&& ae.getErrorCode() == AuthErrorCode.USER_001)
				.verify();

		verify(setOperations, never()).add(anyString(), anyString());
	}

	@Test
	void shouldThrowWhenPasswordUpdateAffectsZeroRowsOnChangePassword() {
		ChangePasswordRequest request = new ChangePasswordRequest("CurrentPass123!", "NewPass456789!");
		User user = new User(UUID.randomUUID(), "user@occupify.com", "hashed-current-pass", "USER", "ACTIVE",
				Instant.now(), Instant.now());

		when(userRepository.findByEmail("user@occupify.com")).thenReturn(Mono.just(user));
		when(passwordEncoder.matches("CurrentPass123!", "hashed-current-pass")).thenReturn(true);
		when(passwordEncoder.encode("NewPass456789!")).thenReturn("hashed-new-pass");
		when(userRepository.updatePasswordByEmail("user@occupify.com", "hashed-new-pass"))
				.thenReturn(Mono.just(0));

		StepVerifier.create(authService.changePassword("user@occupify.com", request))
				.expectErrorMatches(ex -> ex instanceof AuthException ae
						&& ae.getErrorCode() == AuthErrorCode.USER_001)
				.verify();

		verify(redisTemplate, never()).execute(eq(revokeAllSessionsScript), anyList());
	}
}
