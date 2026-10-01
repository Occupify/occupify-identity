package com.occupify.identity.service;

import com.occupify.identity.dto.request.ChangePasswordRequest;
import com.occupify.identity.dto.request.ForgotPasswordRequest;
import com.occupify.identity.dto.request.LoginRequest;
import com.occupify.identity.dto.request.RegisterRequest;
import com.occupify.identity.dto.request.ResetPasswordRequest;
import com.occupify.identity.dto.response.AuthResponse;
import com.occupify.identity.dto.response.TokenRefreshResponse;
import reactor.core.publisher.Mono;

public interface AuthService {

    Mono<AuthResponse> register(RegisterRequest request);

    Mono<AuthResponse> login(LoginRequest request);

    Mono<TokenRefreshResponse> refreshToken(String refreshToken);

    Mono<Void> signOut(String refreshToken);

    Mono<Void> forgotPassword(ForgotPasswordRequest request);

    Mono<Void> resetPassword(ResetPasswordRequest request);

    Mono<Void> changePassword(String userEmail, ChangePasswordRequest request);
}
