package com.occupify.identity.dto.response.user;

import org.springframework.lang.Nullable;
import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
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
        Instant updatedAt,
        @Nullable UUID institutionId,
        @Nullable String institutionName
) {
    public UserProfileResponse(UUID id, String email, String fullName, String headline, String avatarUrl,
                               String bio, String phoneNumber, String role, String status, Instant createdAt, Instant updatedAt) {
        this(id, email, fullName, headline, avatarUrl, bio, phoneNumber, role, status, createdAt, updatedAt, null, null);
    }
}
