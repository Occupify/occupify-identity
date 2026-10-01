package com.occupify.identity.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @Schema(example = "candidate@occupify.com")
        @NotBlank(message = "Email must not be blank")
        @Email(message = "Invalid email format")
        String email,

        @Schema(example = "123456")
        @NotBlank(message = "OTP code must not be blank")
        @Pattern(regexp = "^\\d{6}$", message = "OTP must be a 6-digit number")
        String otpCode,

        @Schema(example = "NewSecret123!")
        @NotBlank(message = "New password must not be blank")
        @Size(min = 8, message = "Password must be at least 8 characters long")
        String newPassword
) {
}
