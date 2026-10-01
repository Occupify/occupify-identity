package com.occupify.identity.controller;

import com.occupify.identity.dto.request.ChangePasswordRequest;
import com.occupify.identity.dto.request.ForgotPasswordRequest;
import com.occupify.identity.dto.request.LoginRequest;
import com.occupify.identity.dto.request.RegisterRequest;
import com.occupify.identity.dto.request.ResetPasswordRequest;
import com.occupify.identity.dto.response.AuthResponse;
import com.occupify.identity.dto.response.TokenRefreshResponse;
import com.occupify.identity.dto.response.UserSummaryResponse;
import com.occupify.identity.exception.AuthErrorCode;
import com.occupify.identity.exception.AuthControllerAdvice;
import com.occupify.identity.exception.AuthException;
import com.occupify.identity.jwt.JwtUtils;
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

    private static final String TEST_SECRET = "occupify-super-secret-jwt-signing-key-for-unit-testing-must-be-at-least-64-bytes-long!";

    @Mock
    private AuthService authService;

    private JwtUtils jwtUtils;
    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils(TEST_SECRET, 3600000, 604800000);
        AuthController controller = new AuthController(authService, jwtUtils);
        webTestClient = WebTestClient.bindToController(controller)
                .controllerAdvice(new AuthControllerAdvice())
                .build();
    }

    @Test
    void shouldRegisterAccountWith201Created() {
        RegisterRequest request = new RegisterRequest("new@occupify.com", "Password123!");
        UserSummaryResponse userSummary = new UserSummaryResponse(
                UUID.randomUUID(), "new@occupify.com", "USER", "ACTIVE", Instant.now()
        );
        AuthResponse authResponse = new AuthResponse("token-access", "token-refresh", userSummary);

        when(authService.register(any(RegisterRequest.class))).thenReturn(Mono.just(authResponse));

        webTestClient.post()
                .uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.status").isEqualTo(201)
                .jsonPath("$.message").isEqualTo("Account registered successfully")
                .jsonPath("$.data.accessToken").isEqualTo("token-access")
                .jsonPath("$.data.user.email").isEqualTo("new@occupify.com");
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
                .jsonPath("$.data.accessToken").isEqualTo("new-access-token")
                .jsonPath("$.data.refreshToken").isEqualTo("valid-refresh-token");
    }

    @Test
    void shouldReturn400WhenRefreshTokenHeaderMissing() {
        webTestClient.post()
                .uri("/auth/refresh-token")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.errorCode").isEqualTo("AUTH_004");
    }

    @Test
    void shouldSignOutWith200Ok() {
        when(authService.signOut("valid-refresh-token")).thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/auth/signout")
                .header("X-Refresh-Token", "valid-refresh-token")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo(200)
                .jsonPath("$.message").isEqualTo("Signed out successfully");
    }

    @Test
    void shouldRequestForgotPasswordOtpWith200Ok() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("user@occupify.com");
        when(authService.forgotPassword(any(ForgotPasswordRequest.class))).thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/auth/forget-password")
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
        ResetPasswordRequest request = new ResetPasswordRequest("user@occupify.com", "123456", "NewPassword123!");
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
    void shouldChangePasswordWith200OkWhenUserEmailInjected() {
        ChangePasswordRequest request = new ChangePasswordRequest("OldPassword123!", "NewPassword123!");
        when(authService.changePassword(eq("user@occupify.com"), any(ChangePasswordRequest.class)))
                .thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/auth/change-password")
                .header("X-User-Email", "user@occupify.com")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo(200)
                .jsonPath("$.message").isEqualTo("Password changed successfully");
    }

    @Test
    void shouldChangePasswordWith200OkWhenBearerTokenProvided() {
        ChangePasswordRequest request = new ChangePasswordRequest("OldPassword123!", "NewPassword123!");
        String token = jwtUtils.generateAccessToken("tokenuser@occupify.com", UUID.randomUUID().toString(), "USER");
        when(authService.changePassword(eq("tokenuser@occupify.com"), any(ChangePasswordRequest.class)))
                .thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/auth/change-password")
                .header(org.springframework.http.HttpHeaders.AUTHORIZATION, "Bearer " + token)
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
