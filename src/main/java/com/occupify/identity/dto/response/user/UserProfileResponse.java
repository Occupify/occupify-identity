package com.occupify.identity.dto.response.user;

import org.springframework.lang.Nullable;
import java.time.Instant;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String email,
        @Nullable String fullName,
        @Nullable String headline,
        @Nullable String avatarUrl,
        @Nullable String bio,
        @Nullable String phoneNumber,
        String role,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
