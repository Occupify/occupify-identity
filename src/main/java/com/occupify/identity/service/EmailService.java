package com.occupify.identity.service;

import reactor.core.publisher.Mono;

public interface EmailService {

    Mono<Void> sendRegistrationOtp(String recipientEmail, String otpCode);

    Mono<Void> sendPasswordResetOtp(String recipientEmail, String otpCode);
}
