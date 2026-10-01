package com.occupify.identity.service.client;

import reactor.core.publisher.Mono;

public interface NotificationClient {

    Mono<Void> sendPasswordResetOtp(String recipientEmail, String otpCode);
}
