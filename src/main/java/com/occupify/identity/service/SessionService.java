package com.occupify.identity.service;

import reactor.core.publisher.Mono;

import java.util.UUID;

public interface SessionService {

    record SessionMetadata(UUID userId, String email, long createdAt) {
    }

    Mono<Void> saveSession(String refreshToken, UUID userId, String email);

    Mono<Boolean> isValidSession(String refreshToken);

    Mono<SessionMetadata> getSession(String refreshToken);

    Mono<Void> revokeSession(String refreshToken);

    Mono<Long> revokeAllUserSessions(String email);
}
