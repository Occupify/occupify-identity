package com.occupify.identity.dto.response.user;

import java.time.LocalDate;
import java.util.UUID;

public record ExperienceItemResponse(
        UUID id,
        String title,
        String organization,
        String employmentType,
        String location,
        String locationType,
        LocalDate startDate,
        LocalDate endDate,
        boolean isCurrent,
        String description
) {
}
