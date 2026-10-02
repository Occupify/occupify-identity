package com.occupify.identity.dto.response;

import java.util.UUID;

public record RegisterResponse(
        UUID id,
        String email,
        String status
) {
}
