package com.occupify.identity.dto.request;

import com.occupify.identity.enums.OtpType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SendOtpRequest(
        @NotBlank(message = "Email must not be blank")
        @Email(message = "Invalid email format")
        String email,

        @Schema(description = "Type of OTP: REGISTER or FORGOT_PASSWORD")
        @NotNull(message = "OTP type must not be null")
        OtpType type
) {
}

