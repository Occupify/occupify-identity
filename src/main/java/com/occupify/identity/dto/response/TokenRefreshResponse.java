package com.occupify.identity.dto.response;

public record TokenRefreshResponse(
        String accessToken,
        String refreshToken
) {
}
