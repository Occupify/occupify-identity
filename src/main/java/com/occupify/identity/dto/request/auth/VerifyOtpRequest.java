package com.occupify.identity.dto.request.auth;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.occupify.identity.enums.OtpType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.lang.Nullable;

public record VerifyOtpRequest(
        @NotBlank(message = "Email must not be blank")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "OTP code must not be blank")
        @Pattern(regexp = "^\\d{6}$", message = "OTP must be a 6-digit number")
        @JsonAlias({ "code", "otp" })
        String otpCode,

        @Schema(description = "Type of OTP: REGISTER or FORGOT_PASSWORD. If omitted, automatically determined from pending session.")
        @Nullable
        OtpType type
) {
    public VerifyOtpRequest(String email, String otpCode) {
        this(email, otpCode, null);
    }
}

