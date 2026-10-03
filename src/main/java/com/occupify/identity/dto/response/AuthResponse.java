package com.occupify.identity.dto.response;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        UserSummaryResponse user
) {
}
