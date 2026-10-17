package com.occupify.identity.dto.response.auth;

import com.fasterxml.jackson.annotation.JsonInclude;

import org.springframework.lang.Nullable;

import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserResponse(
        UUID id,
        String email,
        String role,
        String status,
        Instant createdAt,
        @Nullable UUID institutionId,
        @Nullable String institutionName
) {
    public UserResponse(UUID id, String email, String role, String status, Instant createdAt) {
        this(id, email, role, status, createdAt, null, null);
    }
}
