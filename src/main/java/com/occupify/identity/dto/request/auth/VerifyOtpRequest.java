package com.occupify.identity.dto.request.auth;

import com.occupify.identity.enums.OtpType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record VerifyOtpRequest(
        @NotBlank(message = "Email must not be blank")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "OTP code must not be blank")
        @Pattern(regexp = "^\\d{6}$", message = "OTP must be a 6-digit number")
        String otpCode,

        @Schema(description = "Type of OTP: REGISTER or FORGOT_PASSWORD")
        @NotNull(message = "OTP type must not be null")
        OtpType type
) {
}

