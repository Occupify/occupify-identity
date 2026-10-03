package com.occupify.identity.controller;

import com.occupify.identity.dto.request.ForgotPasswordRequest;
import com.occupify.identity.dto.request.LoginRequest;
import com.occupify.identity.dto.request.RegisterRequest;
import com.occupify.identity.dto.request.ResetPasswordRequest;
import com.occupify.identity.dto.request.SendOtpRequest;
import com.occupify.identity.dto.request.VerifyOtpRequest;
import com.occupify.identity.dto.response.AuthResponse;
import com.occupify.identity.dto.response.RegisterResponse;
import com.occupify.identity.dto.response.TokenRefreshResponse;
import com.occupify.identity.dto.response.UserSummaryResponse;
import com.occupify.identity.dto.response.VerifyOtpResponse;
import com.occupify.identity.enums.OtpType;
import com.occupify.identity.exception.AuthErrorCode;
import com.occupify.identity.exception.AuthControllerAdvice;
import com.occupify.identity.exception.AuthException;
import com.occupify.identity.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        AuthController controller = new AuthController(authService);
        webTestClient = WebTestClient.bindToController(controller)
                .controllerAdvice(new AuthControllerAdvice())
                .build();
    }

    @Test
    void shouldRegisterAccountWith201Created() {
        RegisterRequest request = new RegisterRequest("new@occupify.com", "Password123!");
        UUID userId = UUID.randomUUID();
        RegisterResponse registerResponse = new RegisterResponse(userId, "new@occupify.com", "INACTIVE");

        when(authService.register(any(RegisterRequest.class))).thenReturn(Mono.just(registerResponse));

        webTestClient.post()
                .uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.status").isEqualTo(201)
                .jsonPath("$.message").isEqualTo("Account registered successfully. Please verify OTP sent to your email.")
                .jsonPath("$.data.email").isEqualTo("new@occupify.com")
                .jsonPath("$.data.status").isEqualTo("INACTIVE");
    }

    @Test
    void shouldSendOtpWith200Ok() {
        SendOtpRequest request = new SendOtpRequest("new@occupify.com", OtpType.REGISTER);
        when(authService.sendOtp(any(SendOtpRequest.class))).thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/auth/send-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo(200)
                .jsonPath("$.message").isEqualTo("OTP sent to your email successfully");
    }

    @Test
    void shouldResendOtpWith200Ok() {
        SendOtpRequest request = new SendOtpRequest("new@occupify.com", OtpType.REGISTER);
        when(authService.resendOtp(any(SendOtpRequest.class))).thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/auth/resend-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo(200)
                .jsonPath("$.message").isEqualTo("OTP resent to your email successfully");
    }

    @Test
    void shouldVerifyRegisterOtpWith200Ok() {
        VerifyOtpRequest request = new VerifyOtpRequest("user@occupify.com", "123456", OtpType.REGISTER);
        UserSummaryResponse userSummary = new UserSummaryResponse(
                UUID.randomUUID(), "user@occupify.com", "USER", "ACTIVE", Instant.now()
        );
        AuthResponse authResponse = new AuthResponse("token-access", "token-refresh", userSummary);
        when(authService.verifyOtp(any(VerifyOtpRequest.class))).thenReturn(Mono.just(authResponse));

        webTestClient.post()
                .uri("/auth/verify-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo(200)
                .jsonPath("$.message").isEqualTo("OTP verified successfully")
                .jsonPath("$.data.accessToken").isEqualTo("token-access")
                .jsonPath("$.data.user.email").isEqualTo("user@occupify.com");
    }

    @Test
    void shouldVerifyForgotPasswordOtpWith200Ok() {
        VerifyOtpRequest request = new VerifyOtpRequest("user@occupify.com", "123456", OtpType.FORGOT_PASSWORD);
        VerifyOtpResponse verifyResponse = new VerifyOtpResponse("mock-reset-token");
        when(authService.verifyOtp(any(VerifyOtpRequest.class))).thenReturn(Mono.just(verifyResponse));

        webTestClient.post()
                .uri("/auth/verify-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo(200)
                .jsonPath("$.message").isEqualTo("OTP verified successfully")
                .jsonPath("$.data.resetToken").isEqualTo("mock-reset-token");
    }

    @Test
    void shouldLoginWith200Ok() {
        LoginRequest request = new LoginRequest("user@occupify.com", "Password123!");
        UserSummaryResponse userSummary = new UserSummaryResponse(
                UUID.randomUUID(), "user@occupify.com", "USER", "ACTIVE", Instant.now()
        );
        AuthResponse authResponse = new AuthResponse("token-access", "token-refresh", userSummary);

        when(authService.login(any(LoginRequest.class))).thenReturn(Mono.just(authResponse));

        webTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo(200)
                .jsonPath("$.message").isEqualTo("Login successful")
                .jsonPath("$.data.accessToken").isEqualTo("token-access");
    }

    @Test
    void shouldRefreshTokenWith200Ok() {
        TokenRefreshResponse response = new TokenRefreshResponse("new-access-token", "valid-refresh-token");
        when(authService.refreshToken("valid-refresh-token")).thenReturn(Mono.just(response));

        webTestClient.post()
                .uri("/auth/refresh-token")
                .header("X-Refresh-Token", "valid-refresh-token")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo(200)
                .jsonPath("$.data.accessToken").isEqualTo("new-access-token");
    }

    @Test
    void shouldSignOutWith200Ok() {
        when(authService.signOut("valid-refresh-token")).thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/auth/sign-out")
                .header("X-Refresh-Token", "valid-refresh-token")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo(200)
                .jsonPath("$.message").isEqualTo("Signed out successfully");
    }

    @Test
    void shouldSendForgotPasswordOtpWith200Ok() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("user@occupify.com");
        when(authService.forgotPassword(any(ForgotPasswordRequest.class))).thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo(200)
                .jsonPath("$.message").isEqualTo("Password reset OTP sent to your email");
    }

    @Test
    void shouldResetPasswordWith200Ok() {
        ResetPasswordRequest request = new ResetPasswordRequest("user@occupify.com", "reset-token", null, "NewPassword123!");
        when(authService.resetPassword(any(ResetPasswordRequest.class))).thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo(200)
                .jsonPath("$.message").isEqualTo("Password reset successfully. Please sign in with your new password.");
    }

    @Test
    void shouldReturnValidationFailureOnShortPassword() {
        RegisterRequest invalidRequest = new RegisterRequest("user@occupify.com", "short");

        webTestClient.post()
                .uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidRequest)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("AUTH_002");
    }

    @Test
    void shouldReturnValidationFailureOnInvalidEmail() {
        RegisterRequest invalidRequest = new RegisterRequest("not-an-email", "ValidPassword123!");

        webTestClient.post()
                .uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidRequest)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("AUTH_000");
    }

    @Test
    void shouldHandleAuthExceptionThroughAdvice() {
        LoginRequest request = new LoginRequest("user@occupify.com", "Password123!");
        when(authService.login(any(LoginRequest.class)))
                .thenReturn(Mono.error(new AuthException(AuthErrorCode.AUTH_003)));

        webTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.errorCode").isEqualTo("AUTH_003")
                .jsonPath("$.message").isEqualTo("Invalid email or password");
    }
}
