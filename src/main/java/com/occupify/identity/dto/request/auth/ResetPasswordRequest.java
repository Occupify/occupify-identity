package com.occupify.identity.dto.request.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "New password must not be blank")
        @Size(min = 8, message = "Password must be at least 8 characters long")
        @Schema(description = "New password", example = "newPassword123!")
        String newPassword,

        @NotBlank(message = "Confirm password must not be blank")
        @Schema(description = "Confirmation of new password", example = "newPassword123!")
        String confirmPassword,

        @Schema(description = "Reset token obtained from /auth/verify-otp (optional if passed via Authorization Bearer or X-Reset-Token header)", example = "550e8400-e29b-41d4-a716-446655440000")
        String resetToken
) {
}
