package com.occupify.identity.controller.auth;

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
import com.occupify.identity.enums.OtpType;
import com.occupify.identity.exception.GlobalExceptionHandler;
import com.occupify.identity.exception.auth.AuthErrorCode;
import com.occupify.identity.exception.auth.AuthException;
import com.occupify.identity.security.JwtUtils;
import com.occupify.identity.security.impl.JwtUtilsImpl;
import com.occupify.identity.service.auth.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
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

        private static final String TEST_SECRET = "occupify-super-secret-jwt-signing-key-for-unit-testing-must-be-at-least-64-bytes-long!";

        @Mock
        private AuthService authService;

        private JwtUtils jwtUtils;
        private WebTestClient webTestClient;

        @BeforeEach
        void setUp() {
                jwtUtils = new JwtUtilsImpl(TEST_SECRET, 3600000, 604800000);
                AuthController controller = new AuthController(authService, jwtUtils);
                webTestClient = WebTestClient.bindToController(controller)
                                .controllerAdvice(new GlobalExceptionHandler())
                                .build();
        }

        @Test
        void shouldRegisterAccountWith201Created() {
                RegisterRequest request = new RegisterRequest("new@occupify.com", "Password123!");
                UUID userId = UUID.randomUUID();
                UserResponse registerResponse = new UserResponse(userId, "new@occupify.com", "USER", "INACTIVE", Instant.now());

                when(authService.register(any(RegisterRequest.class))).thenReturn(Mono.just(registerResponse));

                webTestClient.post()
                                .uri("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(request)
                                .exchange()
                                .expectStatus().isCreated()
                                .expectBody()
                                .jsonPath("$.status").isEqualTo(201)
                                .jsonPath("$.data.email").isEqualTo("new@occupify.com")
                                .jsonPath("$.data.status").isEqualTo("INACTIVE");
        }

        @Test
        void shouldResendOtpWith200Ok() {
                SendOtpRequest request = new SendOtpRequest("user@occupify.com", OtpType.REGISTER);
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
        void shouldVerifyOtpForForgotPasswordWithResetToken() {
                VerifyOtpRequest request = new VerifyOtpRequest("user@occupify.com", "123456", OtpType.FORGOT_PASSWORD);
                VerifyOtpResponse verifyResponse = new VerifyOtpResponse("reset-token-abc");

                when(authService.verifyOtp(any(VerifyOtpRequest.class))).thenReturn(Mono.just(verifyResponse));

                webTestClient.post()
                                .uri("/auth/verify-otp")
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(request)
                                .exchange()
                                .expectStatus().isOk()
                                .expectBody()
                                .jsonPath("$.status").isEqualTo(200)
                                .jsonPath("$.data.resetToken").isEqualTo("reset-token-abc");
        }

        @Test
        void shouldVerifyOtpForRegisterWithAuthResponse() {
                VerifyOtpRequest request = new VerifyOtpRequest("user@occupify.com", "123456", OtpType.REGISTER);
                UserResponse userSummary = new UserResponse(UUID.randomUUID(), "user@occupify.com", "USER", "ACTIVE",
                                Instant.now());
                AuthResponse authResponse = new AuthResponse("access-token-123", "refresh-token-456", userSummary);

                when(authService.verifyOtp(any(VerifyOtpRequest.class))).thenReturn(Mono.just(authResponse));

                webTestClient.post()
                                .uri("/auth/verify-otp")
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(request)
                                .exchange()
                                .expectStatus().isOk()
                                .expectBody()
                                .jsonPath("$.status").isEqualTo(200)
                                .jsonPath("$.data.accessToken").isEqualTo("access-token-123")
                                .jsonPath("$.data.user.email").isEqualTo("user@occupify.com");
        }

        @Test
        void shouldLoginSuccessfullyWith200Ok() {
                LoginRequest request = new LoginRequest("user@occupify.com", "Password123!");
                UserResponse userSummary = new UserResponse(UUID.randomUUID(), "user@occupify.com", "USER", "ACTIVE",
                                Instant.now());
                AuthResponse authResponse = new AuthResponse("access-token-123", "refresh-token-456", userSummary);

                when(authService.login(any(LoginRequest.class))).thenReturn(Mono.just(authResponse));

                webTestClient.post()
                                .uri("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(request)
                                .exchange()
                                .expectStatus().isOk()
                                .expectBody()
                                .jsonPath("$.status").isEqualTo(200)
                                .jsonPath("$.data.accessToken").isEqualTo("access-token-123")
                                .jsonPath("$.data.user.role").isEqualTo("USER");
        }

        @Test
        void shouldRefreshTokenWith200Ok() {
                AuthResponse refreshResponse = new AuthResponse("new-access-token", "new-refresh-token");
                when(authService.refreshToken("valid-refresh-token")).thenReturn(Mono.just(refreshResponse));

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
        void shouldReturn400WhenRefreshTokenHeaderIsMissing() {
                webTestClient.post()
                                .uri("/auth/refresh-token")
                                .exchange()
                                .expectStatus().isBadRequest()
                                .expectBody()
                                .jsonPath("$.status").isEqualTo(400)
                                .jsonPath("$.errorCode").isEqualTo("AUTH_004");
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
        void shouldReturn400WhenSignOutRefreshTokenIsMissing() {
                webTestClient.post()
                                .uri("/auth/sign-out")
                                .exchange()
                                .expectStatus().isBadRequest()
                                .expectBody()
                                .jsonPath("$.status").isEqualTo(400)
                                .jsonPath("$.errorCode").isEqualTo("AUTH_004");
        }

        @Test
        void shouldForgotPasswordWith200Ok() {
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
                ResetPasswordRequest request = new ResetPasswordRequest("user@occupify.com", "reset-token-123",
                                "NewPassword123!");
                when(authService.resetPassword(any(ResetPasswordRequest.class))).thenReturn(Mono.empty());

                webTestClient.post()
                                .uri("/auth/reset-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(request)
                                .exchange()
                                .expectStatus().isOk()
                                .expectBody()
                                .jsonPath("$.status").isEqualTo(200)
                                .jsonPath("$.message")
                                .isEqualTo("Password reset successfully. Please sign in with your new password.");
        }

        @Test
        void shouldRejectInvalidEmailFormatDuringRegister() {
                RegisterRequest request = new RegisterRequest("invalid-email", "Password123!");

                webTestClient.post()
                                .uri("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(request)
                                .exchange()
                                .expectStatus().isBadRequest()
                                .expectBody()
                                .jsonPath("$.status").isEqualTo(400)
                                .jsonPath("$.errorCode").isEqualTo("AUTH_000");
        }

        @Test
        void shouldRejectShortPasswordDuringRegister() {
                RegisterRequest request = new RegisterRequest("user@occupify.com", "short");

                webTestClient.post()
                                .uri("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(request)
                                .exchange()
                                .expectStatus().isBadRequest()
                                .expectBody()
                                .jsonPath("$.status").isEqualTo(400)
                                .jsonPath("$.errorCode").isEqualTo("AUTH_002");
        }

        @Test
        void shouldHandleValidationErrorsThroughAdvice() {
                ForgotPasswordRequest request = new ForgotPasswordRequest("not-an-email");

                webTestClient.post()
                                .uri("/auth/forgot-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(request)
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

        @Test
        void shouldChangePasswordWith200OkUsingInjectedHeader() {
                ChangePasswordRequest request = new ChangePasswordRequest("OldPassword123!", "NewPassword123!");
                when(authService.changePassword(eq("injected@occupify.com"), any(ChangePasswordRequest.class)))
                                .thenReturn(Mono.empty());

                webTestClient.post()
                                .uri("/auth/change-password")
                                .header("X-User-Email", "injected@occupify.com")
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(request)
                                .exchange()
                                .expectStatus().isOk()
                                .expectBody()
                                .jsonPath("$.status").isEqualTo(200)
                                .jsonPath("$.message").isEqualTo("Password changed successfully");
        }

        @Test
        void shouldChangePasswordWith200OkUsingBearerToken() {
                ChangePasswordRequest request = new ChangePasswordRequest("OldPassword123!", "NewPassword123!");
                String token = jwtUtils.generateAccessToken("tokenuser@occupify.com", UUID.randomUUID().toString(),
                                "USER");
                when(authService.changePassword(eq("tokenuser@occupify.com"), any(ChangePasswordRequest.class)))
                                .thenReturn(Mono.empty());

                webTestClient.post()
                                .uri("/auth/change-password")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(request)
                                .exchange()
                                .expectStatus().isOk()
                                .expectBody()
                                .jsonPath("$.status").isEqualTo(200)
                                .jsonPath("$.message").isEqualTo("Password changed successfully");
        }

        @Test
        void shouldReturn401WhenChangePasswordHasNoIdentityHeadersOrToken() {
                ChangePasswordRequest request = new ChangePasswordRequest("OldPassword123!", "NewPassword123!");

                webTestClient.post()
                                .uri("/auth/change-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(request)
                                .exchange()
                                .expectStatus().isUnauthorized()
                                .expectBody()
                                .jsonPath("$.status").isEqualTo(401)
                                .jsonPath("$.errorCode").isEqualTo("AUTH_003");
        }
}
