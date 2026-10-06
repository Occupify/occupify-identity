package com.occupify.identity.controller.auth;

import com.occupify.identity.config.OpenApiConfig;
import com.occupify.identity.dto.base.ApiResponse;
import com.occupify.identity.dto.request.auth.ChangePasswordRequest;
import com.occupify.identity.dto.request.auth.ForgotPasswordRequest;
import com.occupify.identity.dto.request.auth.LoginRequest;
import com.occupify.identity.dto.request.auth.RegisterRequest;
import com.occupify.identity.dto.request.auth.ResetPasswordRequest;
import com.occupify.identity.dto.request.auth.SendOtpRequest;
import com.occupify.identity.dto.request.auth.VerifyOtpRequest;
import com.occupify.identity.dto.response.auth.AuthResponse;
import com.occupify.identity.dto.response.auth.UserResponse;
import com.occupify.identity.exception.auth.AuthErrorCode;
import com.occupify.identity.exception.auth.AuthException;
import com.occupify.identity.security.AuthenticationGatewayFilter;
import com.occupify.identity.security.JwtUtils;
import com.occupify.identity.service.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Auth & Session APIs")
public class AuthController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;
    private final JwtUtils jwtUtils;

    public AuthController(AuthService authService, JwtUtils jwtUtils) {
        this.authService = authService;
        this.jwtUtils = jwtUtils;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Register",
            description = "Register a new user account and automatically send a verification OTP to the user's email"
    )
    public Mono<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        log.info("[POST /auth/register] Register request for email: {}", request.email());
        return authService.register(request)
                .map(data -> ApiResponse.created("Account registered successfully. Verification OTP has been sent to your email.", data));
    }

    @PostMapping("/resend-otp")
    @Operation(
            summary = "Resend OTP",
            description = "Resend a new 6-digit verification OTP to the user's email subject to cooldown restrictions"
    )
    public Mono<ApiResponse<Void>> resendOtp(@Valid @RequestBody SendOtpRequest request) {
        log.info("[POST /auth/resend-otp] Resend OTP request for email: {}", request.email());
        return authService.resendOtp(request)
                .thenReturn(ApiResponse.ok("OTP resent to your email successfully"));
    }

    @PostMapping("/verify-otp")
    @Operation(
            summary = "Verify OTP",
            description = "Verify submitted OTP code: activates account and returns auth tokens for REGISTER, or returns a reset token for FORGOT_PASSWORD"
    )
    public Mono<ApiResponse<Object>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        log.info("[POST /auth/verify-otp] Verifying OTP for email: {}", request.email());
        return authService.verifyOtp(request)
                .map(data -> ApiResponse.ok("OTP verified successfully", data));
    }

    @PostMapping("/login")
    @Operation(summary = "Login")
    public Mono<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("[POST /auth/login] Login attempt for email: {}", request.email());
        return authService.login(request)
                .map(data -> ApiResponse.ok("Login successful", data));
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Refresh access token")
    public Mono<ApiResponse<AuthResponse>> refreshToken(
            @Parameter(description = "Refresh token", required = true)
            @RequestHeader(value = "X-Refresh-Token", required = false) String refreshToken) {
        log.info("[POST /auth/refresh-token] Refresh token request");
        if (refreshToken == null || refreshToken.isBlank()) {
            return Mono.error(new AuthException(AuthErrorCode.AUTH_004));
        }
        return authService.refreshToken(refreshToken)
                .map(data -> ApiResponse.ok("Token refreshed successfully", data));
    }

    @PostMapping("/sign-out")
    @Operation(summary = "Sign out")
    public Mono<ApiResponse<Void>> signOut(
            @Parameter(description = "Refresh token", required = true)
            @RequestHeader(value = "X-Refresh-Token", required = false) String refreshToken) {
        log.info("[POST /auth/sign-out] Sign out request");
        if (refreshToken == null || refreshToken.isBlank()) {
            return Mono.error(new AuthException(AuthErrorCode.AUTH_004));
        }
        return authService.signOut(refreshToken)
                .thenReturn(ApiResponse.ok("Signed out successfully"));
    }

    @PostMapping("/forgot-password")
    @Operation(
            summary = "Forgot password",
            description = "Initiate password reset and automatically send a reset OTP to the user's email"
    )
    public Mono<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("[POST /auth/forgot-password] Forgot password request for email: {}", request.email());
        return authService.forgotPassword(request)
                .thenReturn(ApiResponse.ok("Password reset OTP sent to your email"));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password")
    public Mono<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("[POST /auth/reset-password] Resetting password for email: {}", request.email());
        return authService.resetPassword(request)
                .thenReturn(ApiResponse.ok("Password reset successfully. Please sign in with your new password."));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change password")
    @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
    public Mono<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            ServerWebExchange exchange) {
        String injectedEmail = exchange.getRequest().getHeaders().getFirst(AuthenticationGatewayFilter.HEADER_USER_EMAIL);
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        String userEmail = resolveUserEmail(injectedEmail, authHeader);
        log.info("[POST /auth/change-password] Changing password for email: {}", userEmail);
        return authService.changePassword(userEmail, request)
                .thenReturn(ApiResponse.ok("Password changed successfully"));
    }

    private String resolveUserEmail(String injectedEmail, String authHeader) {
        if (injectedEmail != null && !injectedEmail.isBlank()) {
            return injectedEmail;
        }
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX) && jwtUtils != null) {
            String token = authHeader.substring(BEARER_PREFIX.length()).trim();
            if (jwtUtils.validateToken(token)) {
                return jwtUtils.extractEmail(token);
            }
        }
        throw new AuthException(AuthErrorCode.AUTH_003, "Authentication token or identity context required");
    }
}
