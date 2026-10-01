package com.occupify.identity.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @Schema(example = "Password123!")
        @NotBlank(message = "Current password must not be blank")
        String currentPassword,

        @Schema(example = "BrandNewPass123!")
        @NotBlank(message = "New password must not be blank")
        @Size(min = 8, message = "Password must be at least 8 characters long")
        String newPassword
) {
}
