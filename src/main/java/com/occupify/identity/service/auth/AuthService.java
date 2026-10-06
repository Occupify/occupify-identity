package com.occupify.identity.service.auth;

import com.occupify.identity.dto.request.auth.ChangePasswordRequest;
import com.occupify.identity.dto.request.auth.ForgotPasswordRequest;
import com.occupify.identity.dto.request.auth.LoginRequest;
import com.occupify.identity.dto.request.auth.RegisterRequest;
import com.occupify.identity.dto.request.auth.ResetPasswordRequest;
import com.occupify.identity.dto.request.auth.SendOtpRequest;
import com.occupify.identity.dto.request.auth.VerifyOtpRequest;
import com.occupify.identity.dto.response.auth.AuthResponse;
import com.occupify.identity.dto.response.auth.UserResponse;
import reactor.core.publisher.Mono;

public interface AuthService {

    Mono<UserResponse> register(RegisterRequest request);

    Mono<AuthResponse> login(LoginRequest request);

    Mono<AuthResponse> refreshToken(String refreshToken);

    Mono<Void> signOut(String refreshToken);

    Mono<Void> resendOtp(SendOtpRequest request);

    Mono<Object> verifyOtp(VerifyOtpRequest request);

    Mono<Void> forgotPassword(ForgotPasswordRequest request);

    Mono<Void> resetPassword(ResetPasswordRequest request);

    Mono<Void> changePassword(String userEmail, ChangePasswordRequest request);
}
