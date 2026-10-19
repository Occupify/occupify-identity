package com.occupify.identity.dto.request.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request body for Google OAuth login")
public record GoogleLoginRequest(
        @NotBlank(message = "Google ID token must not be blank")
        @Schema(description = "Google ID Token (JWT) issued by Google Sign-In", requiredMode = Schema.RequiredMode.REQUIRED)
        String idToken
) {
}
