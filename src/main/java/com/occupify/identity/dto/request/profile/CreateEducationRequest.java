package com.occupify.identity.dto.request.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateEducationRequest(
        @NotBlank(message = "Institution is required")
        @Size(max = 200, message = "Institution must not exceed 200 characters")
        String institution,

        @NotBlank(message = "Field of study is required")
        @Size(max = 150, message = "Field of study must not exceed 150 characters")
        String fieldOfStudy,

        @Size(max = 150, message = "Degree must not exceed 150 characters")
        String degree,

        @Size(max = 50, message = "Grade must not exceed 50 characters")
        String grade,

        String description,

        @NotNull(message = "Start date is required")
        LocalDate startDate,

        LocalDate endDate
) {
}
