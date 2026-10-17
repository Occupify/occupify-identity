package com.occupify.identity.dto.request.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequest(
        @NotBlank(message = "Full name is required")
        @Size(max = 150, message = "Full name must not exceed 150 characters")
        String fullName,

        @Size(max = 255, message = "Headline must not exceed 255 characters")
        String headline,

        String bio,

        @Pattern(regexp = "^[+0-9\\-\\s()]{7,30}$", message = "Invalid phone number format")
        String phoneNumber
) {
}
