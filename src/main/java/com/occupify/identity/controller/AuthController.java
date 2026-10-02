package com.occupify.identity.controller;

import com.occupify.identity.config.OpenApiConfig;
import com.occupify.identity.dto.request.ChangePasswordRequest;
import com.occupify.identity.dto.request.ForgotPasswordRequest;
import com.occupify.identity.dto.request.LoginRequest;
import com.occupify.identity.dto.request.RegisterRequest;
import com.occupify.identity.dto.request.ResetPasswordRequest;
import com.occupify.identity.dto.request.SendOtpRequest;
import com.occupify.identity.dto.request.VerifyOtpRequest;
import com.occupify.identity.dto.response.ApiResponse;
import com.occupify.identity.dto.response.AuthResponse;
import com.occupify.identity.dto.response.RegisterResponse;
import com.occupify.identity.dto.response.TokenRefreshResponse;
import com.occupify.identity.exception.AuthErrorCode;
import com.occupify.identity.exception.AuthException;
import com.occupify.identity.filter.AuthenticationGatewayFilter;
import com.occupify.identity.jwt.JwtUtils;
import com.occupify.identity.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

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
    @Operation(summary = "Register user")
    public Mono<ApiResponse<RegisterResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request)
                .map(data -> ApiResponse.created("Account registered successfully. Please verify OTP sent to your email.", data));
    }

    @PostMapping("/send-otp")
    @Operation(
            summary = "Send OTP",
            description = "Dispatch a 6-digit verification OTP to the user's email for registration activation or password reset"
    )
    public Mono<ApiResponse<Void>> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        return authService.sendOtp(request)
                .thenReturn(ApiResponse.ok("OTP sent to your email successfully"));
    }

    @PostMapping("/resend-otp")
    @Operation(
            summary = "Resend OTP",
            description = "Resend a new 6-digit verification OTP to the user's email subject to cooldown restrictions"
    )
    public Mono<ApiResponse<Void>> resendOtp(@Valid @RequestBody SendOtpRequest request) {
        return authService.resendOtp(request)
                .thenReturn(ApiResponse.ok("OTP resent to your email successfully"));
    }

    @PostMapping("/verify-otp")
    @Operation(
            summary = "Verify OTP",
            description = "Verify submitted OTP code: activates account and returns auth tokens for REGISTER, or returns a reset token for FORGOT_PASSWORD"
    )
    public Mono<ApiResponse<Object>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return authService.verifyOtp(request)
                .map(data -> ApiResponse.ok("OTP verified successfully", data));
    }

    @PostMapping("/login")
    @Operation(summary = "Login")
    public Mono<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request)
                .map(data -> ApiResponse.ok("Login successful", data));
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Refresh access token")
    public Mono<ApiResponse<TokenRefreshResponse>> refreshToken(
            @Parameter(description = "Refresh token", required = true)
            @RequestHeader(value = "X-Refresh-Token", required = false) String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return Mono.error(new AuthException(AuthErrorCode.AUTH_004));
        }
        return authService.refreshToken(refreshToken)
                .map(data -> ApiResponse.ok("Token refreshed successfully", data));
    }

    @PostMapping("/signout")
    @Operation(summary = "Sign out")
    public Mono<ApiResponse<Void>> signOut(
            @Parameter(description = "Refresh token", required = true)
            @RequestHeader(value = "X-Refresh-Token", required = false) String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return Mono.error(new AuthException(AuthErrorCode.AUTH_004));
        }
        return authService.signOut(refreshToken)
                .thenReturn(ApiResponse.ok("Signed out successfully"));
    }

    @PostMapping("/forget-password")
    @Operation(summary = "Forgot password")
    public Mono<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return authService.forgotPassword(request)
                .thenReturn(ApiResponse.ok("Password reset OTP sent to your email"));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password")
    public Mono<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return authService.resetPassword(request)
                .thenReturn(ApiResponse.ok("Password reset successfully. Please sign in with your new password."));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change password")
    @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
    public Mono<ApiResponse<Void>> changePassword(
            @RequestHeader(value = AuthenticationGatewayFilter.HEADER_USER_EMAIL, required = false) String injectedEmail,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @Valid @RequestBody ChangePasswordRequest request) {
        String userEmail = resolveUserEmail(injectedEmail, authHeader);
        return authService.changePassword(userEmail, request)
                .thenReturn(ApiResponse.ok("Password changed successfully"));
    }

    private String resolveUserEmail(String injectedEmail, String authHeader) {
        if (injectedEmail != null && !injectedEmail.isBlank()) {
            return injectedEmail;
        }
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            String token = authHeader.substring(BEARER_PREFIX.length()).trim();
            if (jwtUtils.validateToken(token)) {
                return jwtUtils.extractEmail(token);
            }
        }
        throw new AuthException(AuthErrorCode.AUTH_003, "Authentication token or identity context required");
    }
}
