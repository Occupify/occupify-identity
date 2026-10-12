package com.occupify.identity.controller.auth;

import com.occupify.identity.config.OpenApiConfig;
import com.occupify.identity.controller.AbstractBaseController;
import com.occupify.identity.dto.base.CreatedResponse;
import com.occupify.identity.dto.base.SingleResponse;
import com.occupify.identity.dto.base.SuccessResponse;
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
import com.occupify.identity.exception.auth.AuthErrorCode;
import com.occupify.identity.exception.auth.AuthException;
import com.occupify.identity.security.JwtUtils;
import com.occupify.identity.service.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static com.occupify.identity.constant.SecurityConstants.BEARER_PREFIX;
import static com.occupify.identity.constant.SecurityConstants.HEADER_REFRESH_TOKEN;
import static com.occupify.identity.constant.SecurityConstants.HEADER_USER_EMAIL;

@Slf4j
@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Endpoints for user authentication and session management")
public class AuthController extends AbstractBaseController {

    private final AuthService authService;
    private final JwtUtils jwtUtils;

    public AuthController(AuthService authService, JwtUtils jwtUtils) {
        this.authService = authService;
        this.jwtUtils = jwtUtils;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register", description = "Register a new user account and automatically send a verification OTP to the user's email")
    public CreatedResponse<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("[POST /auth/register] Register request for email: {}", request.email());
        UserResponse data = authService.register(request);
        return created(data, "Account registered successfully. Verification OTP has been sent to your email.");
    }

    @PostMapping("/resend-otp")
    @Operation(summary = "Resend OTP", description = "Resend a new 6-digit verification OTP to the user's email subject to cooldown restrictions")
    public SuccessResponse resendOtp(@Valid @RequestBody SendOtpRequest request) {
        log.info("[POST /auth/resend-otp] Resend OTP request for email: {}", request.email());
        authService.resendOtp(request);
        return success("OTP resent to your email successfully");
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify OTP", description = "Verify submitted OTP code: activates account and returns auth tokens for REGISTER, or returns a reset token for FORGOT_PASSWORD")
    public SingleResponse<Object> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        log.info("[POST /auth/verify-otp] Verifying OTP for email: {}", request.email());
        Object data = authService.verifyOtp(request);
        return successSingle(data, "OTP verified successfully");
    }

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Authenticate user using email and password")
    public SingleResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("[POST /auth/login] Login attempt for email: {}", request.email());
        AuthResponse data = authService.login(request);
        return successSingle(data, "Login successful");
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Refresh access token", description = "Obtain a new access token using a valid refresh token")
    public SingleResponse<AuthResponse> refreshToken(
            @Parameter(description = "Refresh token in header (optional if provided in body or Authorization header)")
            @RequestHeader(value = HEADER_REFRESH_TOKEN, required = false) String refreshTokenHeader,
            @RequestBody(required = false) RefreshTokenRequest requestBody,
            HttpServletRequest request) {
        log.info("[POST /auth/refresh-token] Refresh token request");
        String refreshToken = resolveRefreshToken(refreshTokenHeader, requestBody, request);
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new AuthException(AuthErrorCode.AUTH_004);
        }
        AuthResponse data = authService.refreshToken(refreshToken);
        return successSingle(data, "Token refreshed successfully");
    }

    @PostMapping("/sign-out")
    @Operation(summary = "Sign out", description = "Logout the user and invalidate active session tokens")
    public SuccessResponse signOut(
            @Parameter(description = "Refresh token in header (optional if provided in body or Authorization header)")
            @RequestHeader(value = HEADER_REFRESH_TOKEN, required = false) String refreshTokenHeader,
            @RequestBody(required = false) RefreshTokenRequest requestBody,
            HttpServletRequest request) {
        log.info("[POST /auth/sign-out] Sign out request");
        String refreshToken = resolveRefreshToken(refreshTokenHeader, requestBody, request);
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new AuthException(AuthErrorCode.AUTH_004);
        }
        authService.signOut(refreshToken);
        return success("Signed out successfully");
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Forgot password", description = "Initiate password reset and automatically send a reset OTP to the user's email")
    public SuccessResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("[POST /auth/forgot-password] Forgot password request for email: {}", request.email());
        authService.forgotPassword(request);
        return success("Password reset OTP sent to your email");
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password", description = "Reset user password using reset token obtained from /auth/verify-otp, new password, and confirm password")
    public SuccessResponse resetPassword(
            @Valid @RequestBody ResetPasswordRequest request,
            @Parameter(description = "Reset token (optional if provided in request body)")
            @RequestHeader(value = "X-Reset-Token", required = false) @Nullable String resetTokenHeader,
            @Nullable HttpServletRequest servletRequest) {
        String authHeader = servletRequest != null ? servletRequest.getHeader(HttpHeaders.AUTHORIZATION) : null;
        String resolvedToken = resolveResetToken(request.resetToken(), resetTokenHeader, authHeader);
        log.info("[POST /auth/reset-password] Reset password request");
        authService.resetPassword(resolvedToken, request);
        return success("Password reset successfully. Please sign in with your new password.");
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change password", description = "Change user password for authenticated account")
    @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
    public SuccessResponse changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @Nullable HttpServletRequest servletRequest) {
        String injectedEmail = servletRequest != null ? servletRequest.getHeader(HEADER_USER_EMAIL) : null;
        String authHeader = servletRequest != null ? servletRequest.getHeader(HttpHeaders.AUTHORIZATION) : null;
        String userEmail = resolveUserEmail(injectedEmail, authHeader);
        log.info("[POST /auth/change-password] Changing password for email: {}", userEmail);
        authService.changePassword(userEmail, request);
        return success("Password changed successfully");
    }

    @Nullable
    private String resolveRefreshToken(@Nullable String headerToken, @Nullable RefreshTokenRequest bodyRequest,
                                        @Nullable HttpServletRequest request) {
        String rawToken = null;
        if (bodyRequest != null && bodyRequest.refreshToken() != null && !bodyRequest.refreshToken().isBlank()) {
            rawToken = bodyRequest.refreshToken();
        } else if (headerToken != null && !headerToken.isBlank()) {
            rawToken = headerToken;
        } else if (request != null) {
            String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (authHeader != null && !authHeader.isBlank()) {
                if (authHeader.startsWith("Bearer ")) {
                    rawToken = authHeader.substring(7).trim();
                } else {
                    rawToken = authHeader.trim();
                }
            }
        }
        return cleanToken(rawToken);
    }

    @Nullable
    private String cleanToken(@Nullable String token) {
        if (token == null) {
            return null;
        }
        String cleaned = token.trim();
        while ((cleaned.startsWith("\"") && cleaned.endsWith("\""))
                || (cleaned.startsWith("'") && cleaned.endsWith("'"))) {
            cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
        }
        if (cleaned.startsWith(BEARER_PREFIX)) {
            cleaned = cleaned.substring(BEARER_PREFIX.length()).trim();
        }
        return cleaned.isBlank() ? null : cleaned;
    }

    @Nullable
    private String resolveResetToken(@Nullable String bodyToken, @Nullable String headerToken, @Nullable String authHeader) {
        if (bodyToken != null && !bodyToken.isBlank()) {
            return cleanToken(bodyToken);
        }
        if (headerToken != null && !headerToken.isBlank()) {
            return cleanToken(headerToken);
        }
        if (authHeader != null && !authHeader.isBlank()) {
            return cleanToken(authHeader);
        }
        return null;
    }

    private String resolveUserEmail(@Nullable String injectedEmail, @Nullable String authHeader) {
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
