package com.occupify.identity.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(
        @Schema(example = "candidate@occupify.com")
        @NotBlank(message = "Email must not be blank")
        @Email(message = "Invalid email format")
        String email
) {
}
