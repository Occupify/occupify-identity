package com.occupify.identity.dto.request.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "Email must not be blank")
        @Email(message = "Invalid email format")
        String email,

        @Schema(description = "Reset token obtained from /auth/verify-otp")
        String resetToken,

        @Schema(description = "Optional legacy OTP code if not using resetToken")
        String otpCode,

        @NotBlank(message = "New password must not be blank")
        @Size(min = 8, message = "Password must be at least 8 characters long")
        String newPassword
) {
    public ResetPasswordRequest(String email, String otpCode, String newPassword) {
        this(email, null, otpCode, newPassword);
    }
}

