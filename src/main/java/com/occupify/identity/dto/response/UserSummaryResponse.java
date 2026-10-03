package com.occupify.identity.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserSummaryResponse(
        UUID id,
        String email,
        String role,
        String status,
        Instant createdAt
) {
}
