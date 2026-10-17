package com.occupify.identity.dto.response.user;

import org.springframework.lang.Nullable;
import java.time.LocalDate;
import java.util.UUID;

public record ExperienceItemResponse(
        UUID id,
        String title,
        String organization,
        @Nullable String employmentType,
        @Nullable String location,
        @Nullable String locationType,
        LocalDate startDate,
        @Nullable LocalDate endDate,
        boolean isCurrent,
        @Nullable String description
) {
}
