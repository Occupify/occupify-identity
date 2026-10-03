package com.occupify.identity.service;

import com.occupify.identity.enums.OtpType;
import reactor.core.publisher.Mono;

public interface OtpService {

    record OtpData(String otpCode, int attempts, long createdAt) {
    }

    Mono<String> generateAndStoreOtp(String email, OtpType type);

    Mono<Void> verifyOtp(String email, String rawOtp, OtpType type);

    Mono<String> createPasswordResetToken(String email);

    Mono<String> validatePasswordResetToken(String email, String resetToken);

    Mono<Void> deletePasswordResetToken(String resetToken);

    default Mono<String> generateAndStoreOtp(String email) {
        return generateAndStoreOtp(email, OtpType.FORGOT_PASSWORD);
    }

    default Mono<Void> verifyOtp(String email, String rawOtp) {
        return verifyOtp(email, rawOtp, OtpType.FORGOT_PASSWORD);
    }
}
