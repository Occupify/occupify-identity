package com.occupify.identity.dto.request.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

public record CreateQualificationRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must not exceed 200 characters")
        String title,

        @NotBlank(message = "Organization is required")
        @Size(max = 200, message = "Organization must not exceed 200 characters")
        String organization,

        @NotNull(message = "Issue date is required")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate issueDate,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate expirationDate,

        @Size(max = 100, message = "Credential ID must not exceed 100 characters")
        String credentialId,

        @Size(max = 500, message = "Credential URL must not exceed 500 characters")
        String credentialUrl,

        @Nullable
        MultipartFile document
) {
}
