package com.occupify.identity.dto.request.auth;

import io.swagger.v3.oas.annotations.media.Schema;

public record RefreshTokenRequest(
                @Schema(description = "Refresh token") String refreshToken) {
}
