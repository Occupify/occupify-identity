package com.occupify.identity.dto.request.auth;

import com.occupify.identity.enums.OtpType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.lang.Nullable;

public record SendOtpRequest(
        @NotBlank(message = "Email must not be blank")
        @Email(message = "Invalid email format")
        String email,

        @Schema(description = "Type of OTP: REGISTER or FORGOT_PASSWORD. If omitted, automatically determined from pending session.")
        @Nullable
        OtpType type
) {
    public SendOtpRequest(String email) {
        this(email, null);
    }
}

