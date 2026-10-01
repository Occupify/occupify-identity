package com.occupify.identity.service;

import reactor.core.publisher.Mono;

public interface OtpService {

    record OtpData(String otpCode, int attempts, long createdAt) {
    }

    Mono<String> generateAndStoreOtp(String email);

    Mono<Void> verifyOtp(String email, String rawOtp);
}
