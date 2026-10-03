package com.occupify.identity.controller;

import com.occupify.identity.config.OpenApiConfig;
import com.occupify.identity.dto.request.ChangePasswordRequest;
import com.occupify.identity.dto.response.ApiResponse;
import com.occupify.identity.exception.AuthErrorCode;
import com.occupify.identity.exception.AuthException;
import com.occupify.identity.filter.AuthenticationGatewayFilter;
import com.occupify.identity.jwt.JwtUtils;
import com.occupify.identity.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/account")
@Tag(name = "Account", description = "Account & User Profile APIs")
public class AccountController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;
    private final JwtUtils jwtUtils;

    public AccountController(AuthService authService, JwtUtils jwtUtils) {
        this.authService = authService;
        this.jwtUtils = jwtUtils;
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
