package com.occupify.identity.controller.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.occupify.identity.dto.request.auth.ChangePasswordRequest;
import com.occupify.identity.dto.request.auth.ForgotPasswordRequest;
import com.occupify.identity.dto.request.auth.LoginRequest;
import com.occupify.identity.dto.request.auth.RefreshTokenRequest;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

        private static final String TEST_SECRET = "occupify-super-secret-jwt-signing-key-for-unit-testing-must-be-at-least-64-bytes-long!";

        @Mock
        private AuthService authService;

        private JwtUtils jwtUtils;
        private MockMvc mockMvc;
        private final ObjectMapper objectMapper = new ObjectMapper();

        @BeforeEach
        void setUp() {
                jwtUtils = new JwtUtilsImpl(TEST_SECRET, 3600000, 604800000);
                AuthController controller = new AuthController(authService, jwtUtils);
                mockMvc = MockMvcBuilders.standaloneSetup(controller)
                                .setControllerAdvice(new GlobalExceptionHandler())
                                .build();
        }

        @Test
        void shouldRegisterAccountWith201Created() throws Exception {
                RegisterRequest request = new RegisterRequest("new@occupify.com", "Password123!");
                UUID userId = UUID.randomUUID();
                UserResponse registerResponse = new UserResponse(userId, "new@occupify.com", "USER", "INACTIVE",
                                Instant.now());

                when(authService.register(any(RegisterRequest.class))).thenReturn(registerResponse);

                mockMvc.perform(post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.statusCode").value(201))
                                .andExpect(jsonPath("$.data.email").value("new@occupify.com"))
                                .andExpect(jsonPath("$.data.status").value("INACTIVE"));
        }

        @Test
        void shouldResendOtpWith200Ok() throws Exception {
                SendOtpRequest request = new SendOtpRequest("user@occupify.com", OtpType.REGISTER);
                doNothing().when(authService).resendOtp(any(SendOtpRequest.class));

                mockMvc.perform(post("/auth/resend-otp")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.statusCode").value(200))
                                .andExpect(jsonPath("$.message").value("OTP resent to your email successfully"));
        }

        @Test
        void shouldVerifyOtpForForgotPasswordWithResetToken() throws Exception {
                VerifyOtpRequest request = new VerifyOtpRequest("user@occupify.com", "123456", OtpType.FORGOT_PASSWORD);
                VerifyOtpResponse verifyResponse = new VerifyOtpResponse("reset-token-abc");

                when(authService.verifyOtp(any(VerifyOtpRequest.class))).thenReturn(verifyResponse);

                mockMvc.perform(post("/auth/verify-otp")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.statusCode").value(200))
                                .andExpect(jsonPath("$.data.resetToken").value("reset-token-abc"));
        }

        @Test
        void shouldVerifyOtpForRegisterWithAuthResponse() throws Exception {
                VerifyOtpRequest request = new VerifyOtpRequest("user@occupify.com", "123456", OtpType.REGISTER);
                UserResponse userSummary = new UserResponse(UUID.randomUUID(), "user@occupify.com", "USER", "ACTIVE",
                                Instant.now());
                AuthResponse authResponse = new AuthResponse("access-token-123", "refresh-token-456", userSummary);

                when(authService.verifyOtp(any(VerifyOtpRequest.class))).thenReturn(authResponse);

                mockMvc.perform(post("/auth/verify-otp")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.statusCode").value(200))
                                .andExpect(jsonPath("$.data.accessToken").value("access-token-123"))
                                .andExpect(jsonPath("$.data.user.email").value("user@occupify.com"));
        }

        @Test
        void shouldLoginSuccessfullyWith200Ok() throws Exception {
                LoginRequest request = new LoginRequest("user@occupify.com", "Password123!");
                UserResponse userSummary = new UserResponse(UUID.randomUUID(), "user@occupify.com", "USER", "ACTIVE",
                                Instant.now());
                AuthResponse authResponse = new AuthResponse("access-token-123", "refresh-token-456", userSummary);

                when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

                mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.statusCode").value(200))
                                .andExpect(jsonPath("$.data.accessToken").value("access-token-123"))
                                .andExpect(jsonPath("$.data.user.role").value("USER"));
        }

        @Test
        void shouldRefreshTokenWith200Ok() throws Exception {
                AuthResponse refreshResponse = new AuthResponse("new-access-token", "new-refresh-token");
                when(authService.refreshToken("valid-refresh-token")).thenReturn(refreshResponse);

                mockMvc.perform(post("/auth/refresh-token")
                                .header("X-Refresh-Token", "valid-refresh-token"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.statusCode").value(200))
                                .andExpect(jsonPath("$.data.accessToken").value("new-access-token"));
        }

        @Test
        void shouldRefreshTokenWith200OkViaRequestBody() throws Exception {
                RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");
                AuthResponse refreshResponse = new AuthResponse("new-access-token", "new-refresh-token");
                when(authService.refreshToken("valid-refresh-token")).thenReturn(refreshResponse);

                mockMvc.perform(post("/auth/refresh-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.statusCode").value(200))
                                .andExpect(jsonPath("$.data.accessToken").value("new-access-token"));
        }

        @Test
        void shouldRefreshTokenWith200OkViaAuthorizationHeader() throws Exception {
                AuthResponse refreshResponse = new AuthResponse("new-access-token", "new-refresh-token");
                when(authService.refreshToken("valid-refresh-token")).thenReturn(refreshResponse);

                mockMvc.perform(post("/auth/refresh-token")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer valid-refresh-token"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.statusCode").value(200))
                                .andExpect(jsonPath("$.data.accessToken").value("new-access-token"));
        }

        @Test
        void shouldReturn400WhenRefreshTokenHeaderIsMissing() throws Exception {
                mockMvc.perform(post("/auth/refresh-token"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.statusCode").value(400))
                                .andExpect(jsonPath("$.errorCode").value("AUTH_004"));
        }

        @Test
        void shouldSignOutWith200Ok() throws Exception {
                doNothing().when(authService).signOut("valid-refresh-token");

                mockMvc.perform(post("/auth/sign-out")
                                .header("X-Refresh-Token", "valid-refresh-token"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.statusCode").value(200))
                                .andExpect(jsonPath("$.message").value("Signed out successfully"));
        }

        @Test
        void shouldReturn400WhenSignOutRefreshTokenIsMissing() throws Exception {
                mockMvc.perform(post("/auth/sign-out"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.statusCode").value(400))
                                .andExpect(jsonPath("$.errorCode").value("AUTH_004"));
        }

        @Test
        void shouldForgotPasswordWith200Ok() throws Exception {
                ForgotPasswordRequest request = new ForgotPasswordRequest("user@occupify.com");
                doNothing().when(authService).forgotPassword(any(ForgotPasswordRequest.class));

                mockMvc.perform(post("/auth/forgot-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.statusCode").value(200))
                                .andExpect(jsonPath("$.message").value("Password reset OTP sent to your email"));
        }

        @Test
        void shouldResetPasswordWith200Ok() throws Exception {
                ResetPasswordRequest request = new ResetPasswordRequest("NewPassword123!", "NewPassword123!",
                                "reset-token-123");
                doNothing().when(authService).resetPassword(eq("reset-token-123"), any(ResetPasswordRequest.class));

                mockMvc.perform(post("/auth/reset-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.statusCode").value(200))
                                .andExpect(jsonPath("$.message")
                                                .value("Password reset successfully. Please sign in with your new password."));
        }

        @Test
        void shouldRejectInvalidEmailFormatDuringRegister() throws Exception {
                RegisterRequest request = new RegisterRequest("invalid-email", "Password123!");

                mockMvc.perform(post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.statusCode").value(400))
                                .andExpect(jsonPath("$.errorCode").value("AUTH_000"));
        }

        @Test
        void shouldRejectShortPasswordDuringRegister() throws Exception {
                RegisterRequest request = new RegisterRequest("user@occupify.com", "short");

                mockMvc.perform(post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.statusCode").value(400))
                                .andExpect(jsonPath("$.errorCode").value("AUTH_002"));
        }

        @Test
        void shouldHandleValidationErrorsThroughAdvice() throws Exception {
                ForgotPasswordRequest request = new ForgotPasswordRequest("not-an-email");

                mockMvc.perform(post("/auth/forgot-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.statusCode").value(400))
                                .andExpect(jsonPath("$.errorCode").value("AUTH_000"));
        }

        @Test
        void shouldHandleAuthExceptionThroughAdvice() throws Exception {
                LoginRequest request = new LoginRequest("user@occupify.com", "Password123!");
                when(authService.login(any(LoginRequest.class)))
                                .thenThrow(new AuthException(AuthErrorCode.AUTH_003));

                mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.statusCode").value(401))
                                .andExpect(jsonPath("$.errorCode").value("AUTH_003"))
                                .andExpect(jsonPath("$.message").value("Invalid email or password"));
        }

        @Test
        void shouldChangePasswordWith200OkUsingInjectedHeader() throws Exception {
                ChangePasswordRequest request = new ChangePasswordRequest("OldPassword123!", "NewPassword123!");
                doNothing().when(authService).changePassword(eq("injected@occupify.com"),
                                any(ChangePasswordRequest.class));

                mockMvc.perform(post("/auth/change-password")
                                .header("X-User-Email", "injected@occupify.com")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.statusCode").value(200))
                                .andExpect(jsonPath("$.message").value("Password changed successfully"));
        }

        @Test
        void shouldChangePasswordWith200OkUsingBearerToken() throws Exception {
                ChangePasswordRequest request = new ChangePasswordRequest("OldPassword123!", "NewPassword123!");
                String token = jwtUtils.generateAccessToken("tokenuser@occupify.com", UUID.randomUUID().toString(),
                                "USER");
                doNothing().when(authService).changePassword(eq("tokenuser@occupify.com"),
                                any(ChangePasswordRequest.class));

                mockMvc.perform(post("/auth/change-password")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.statusCode").value(200))
                                .andExpect(jsonPath("$.message").value("Password changed successfully"));
        }

        @Test
        void shouldReturn401WhenChangePasswordHasNoIdentityHeadersOrToken() throws Exception {
                ChangePasswordRequest request = new ChangePasswordRequest("OldPassword123!", "NewPassword123!");

                mockMvc.perform(post("/auth/change-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.statusCode").value(401))
                                .andExpect(jsonPath("$.errorCode").value("AUTH_003"));
        }
}
