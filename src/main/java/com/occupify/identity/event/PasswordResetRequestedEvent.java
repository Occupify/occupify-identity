package com.occupify.identity.event;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

public record PasswordResetRequestedEvent(
        UUID userId,
        String email,
        String otpCode,
        Instant occurredAt
) implements Serializable {

    public PasswordResetRequestedEvent {
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }
}
