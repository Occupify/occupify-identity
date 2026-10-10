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

public interface AuthService {

    UserResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(String refreshToken);

    void signOut(String refreshToken);

    void resendOtp(SendOtpRequest request);

    Object verifyOtp(VerifyOtpRequest request);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(String resetToken, ResetPasswordRequest request);

    void changePassword(String userEmail, ChangePasswordRequest request);
}
