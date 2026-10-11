package com.occupify.identity.dto.response.user;

import java.time.Instant;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String email,
        String fullName,
        String headline,
        String avatarUrl,
        String bio,
        String phoneNumber,
        String role,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
