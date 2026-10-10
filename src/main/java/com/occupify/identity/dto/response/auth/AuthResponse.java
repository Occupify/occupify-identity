package com.occupify.identity.dto.response.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.lang.Nullable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthResponse(
        String accessToken,
        String refreshToken,
        @Nullable UserResponse user
) {
    public AuthResponse(String accessToken, String refreshToken) {
        this(accessToken, refreshToken, null);
    }
}
