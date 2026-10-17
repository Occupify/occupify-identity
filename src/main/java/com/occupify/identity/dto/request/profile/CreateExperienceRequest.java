package com.occupify.identity.dto.request.profile;

import com.occupify.identity.enums.EmploymentType;
import com.occupify.identity.enums.LocationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateExperienceRequest(
        @NotBlank(message = "Job title is required")
        @Size(max = 150, message = "Job title must not exceed 150 characters")
        String title,

        @NotBlank(message = "Organization is required")
        @Size(max = 200, message = "Organization must not exceed 200 characters")
        String organization,

        EmploymentType employmentType,

        @Size(max = 150, message = "Location must not exceed 150 characters")
        String location,

        LocationType locationType,

        @NotNull(message = "Start date is required")
        LocalDate startDate,

        LocalDate endDate,
        boolean isCurrent,
        String description
) {
}
