package com.occupify.identity.service.client;

import reactor.core.publisher.Mono;

public interface NotificationClient {

    Mono<Void> sendRegistrationOtp(String recipientEmail, String otpCode);

    Mono<Void> sendPasswordResetOtp(String recipientEmail, String otpCode);
}
