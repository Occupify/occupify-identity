package com.occupify.identity.dto.response.auth;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthResponse(
        String accessToken,
        String refreshToken,
        UserResponse user
) {
    public AuthResponse(String accessToken, String refreshToken) {
        this(accessToken, refreshToken, null);
    }
}
