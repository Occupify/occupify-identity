package com.occupify.identity.dto.request.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.UUID;

public record UpdateEducationRequest(
        @NotNull(message = "Institution ID is required")
        UUID institutionId,

        @NotBlank(message = "Field of study is required")
        @Size(max = 150, message = "Field of study must not exceed 150 characters")
        String fieldOfStudy,

        @Size(max = 150, message = "Degree must not exceed 150 characters")
        String degree,

        @Size(max = 50, message = "Grade must not exceed 50 characters")
        String grade,

        String description,

        @NotNull(message = "Start date is required")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate startDate,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate endDate,

        @Nullable
        MultipartFile document,

        @Nullable
        Boolean removeDocument
) {
}
