package com.occupify.identity.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.occupify.identity.enums.OtpType;
import com.occupify.identity.exception.AuthErrorCode;
import com.occupify.identity.exception.AuthException;
import com.occupify.identity.service.impl.RedisOtpServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisOtpServiceImplTest {

    @Mock
    private ReactiveRedisOperations<String, String> redisTemplate;

    @Mock
    private ReactiveValueOperations<String, String> valueOperations;

    @Mock
    private PasswordEncoder passwordEncoder;

    private ObjectMapper objectMapper;
    private RedisOtpServiceImpl otpService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        otpService = new RedisOtpServiceImpl(
                redisTemplate,
                passwordEncoder,
                objectMapper,
                6,
                300L,
                3,
                60L
        );
    }

    @Test
    void shouldGenerateAndStoreRegistrationOtpSuccessfully() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(Mono.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashedOtp");
        when(valueOperations.set(anyString(), anyString(), any(Duration.class))).thenReturn(Mono.just(true));

        StepVerifier.create(otpService.generateAndStoreOtp("user@occupify.com", OtpType.REGISTER))
                .assertNext(otp -> {
                    assertNotNull(otp);
                    assertEquals(6, otp.length());
                    assertTrue(otp.matches("^\\d{6}$"));
                })
                .verifyComplete();

        verify(valueOperations).set(eq("otp:register:user@occupify.com"), anyString(), eq(Duration.ofSeconds(300)));
    }

    @Test
    void shouldGenerateAndStoreForgotPasswordOtpSuccessfully() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(Mono.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashedOtp");
        when(valueOperations.set(anyString(), anyString(), any(Duration.class))).thenReturn(Mono.just(true));

        StepVerifier.create(otpService.generateAndStoreOtp("user@occupify.com", OtpType.FORGOT_PASSWORD))
                .assertNext(otp -> {
                    assertNotNull(otp);
                    assertEquals(6, otp.length());
                    assertTrue(otp.matches("^\\d{6}$"));
                })
                .verifyComplete();

        verify(valueOperations).set(eq("otp:forgot_password:user@occupify.com"), anyString(), eq(Duration.ofSeconds(300)));
    }

    @Test
    void shouldThrowCooldownExceptionWhenOtpRequestedTooSoon() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        long recentTime = System.currentTimeMillis() - 20000; // 20s ago, cooldown is 60s
        String json = "{\"otpCode\":\"$2a$10$hashed\",\"attempts\":0,\"createdAt\":" + recentTime + "}";
        when(valueOperations.get(anyString())).thenReturn(Mono.just(json));

        StepVerifier.create(otpService.generateAndStoreOtp("user@occupify.com", OtpType.FORGOT_PASSWORD))
                .expectErrorMatches(ex -> ex instanceof AuthException ae && ae.getErrorCode() == AuthErrorCode.AUTH_007)
                .verify();
    }

    @Test
    void shouldVerifyOtpSuccessfullyAndCleanUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        String json = "{\"otpCode\":\"$2a$10$hashed\",\"attempts\":0,\"createdAt\":1000}";
        when(valueOperations.get("otp:register:user@occupify.com")).thenReturn(Mono.just(json));
        when(passwordEncoder.matches("123456", "$2a$10$hashed")).thenReturn(true);
        when(redisTemplate.delete("otp:register:user@occupify.com")).thenReturn(Mono.just(1L));

        StepVerifier.create(otpService.verifyOtp("user@occupify.com", "123456", OtpType.REGISTER))
                .verifyComplete();

        verify(redisTemplate).delete("otp:register:user@occupify.com");
    }

    @Test
    void shouldIncrementAttemptsOnWrongOtp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        String json = "{\"otpCode\":\"$2a$10$hashed\",\"attempts\":0,\"createdAt\":1000}";
        when(valueOperations.get("otp:forgot_password:user@occupify.com")).thenReturn(Mono.just(json));
        when(passwordEncoder.matches("wrong-otp", "$2a$10$hashed")).thenReturn(false);
        when(redisTemplate.getExpire(anyString())).thenReturn(Mono.just(Duration.ofSeconds(250)));
        when(valueOperations.set(anyString(), anyString(), any(Duration.class))).thenReturn(Mono.just(true));

        StepVerifier.create(otpService.verifyOtp("user@occupify.com", "wrong-otp", OtpType.FORGOT_PASSWORD))
                .expectErrorMatches(ex -> ex instanceof AuthException ae && ae.getErrorCode() == AuthErrorCode.AUTH_009)
                .verify();

        verify(valueOperations).set(eq("otp:forgot_password:user@occupify.com"), contains("\"attempts\":1"), eq(Duration.ofSeconds(250)));
    }

    @Test
    void shouldDeleteOtpAndThrowMaxAttemptsOnThirdFailure() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        String json = "{\"otpCode\":\"$2a$10$hashed\",\"attempts\":2,\"createdAt\":1000}";
        when(valueOperations.get("otp:forgot_password:user@occupify.com")).thenReturn(Mono.just(json));
        when(passwordEncoder.matches("wrong-otp", "$2a$10$hashed")).thenReturn(false);
        when(redisTemplate.delete("otp:forgot_password:user@occupify.com")).thenReturn(Mono.just(1L));

        StepVerifier.create(otpService.verifyOtp("user@occupify.com", "wrong-otp", OtpType.FORGOT_PASSWORD))
                .expectErrorMatches(ex -> ex instanceof AuthException ae && ae.getErrorCode() == AuthErrorCode.AUTH_010)
                .verify();

        verify(redisTemplate).delete("otp:forgot_password:user@occupify.com");
    }

    @Test
    void shouldManagePasswordResetTokenLifecycle() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.set(startsWith("password_reset_token:"), eq("user@occupify.com"), any(Duration.class)))
                .thenReturn(Mono.just(true));
        when(redisTemplate.delete(anyString())).thenReturn(Mono.just(1L));

        StepVerifier.create(otpService.createPasswordResetToken("user@occupify.com"))
                .assertNext(token -> {
                    assertNotNull(token);
                    assertFalse(token.isBlank());
                })
                .verifyComplete();

        when(valueOperations.get("password_reset_token:valid-token")).thenReturn(Mono.just("user@occupify.com"));
        StepVerifier.create(otpService.validatePasswordResetToken("user@occupify.com", "valid-token"))
                .expectNext("user@occupify.com")
                .verifyComplete();

        StepVerifier.create(otpService.deletePasswordResetToken("valid-token"))
                .verifyComplete();
    }
}
