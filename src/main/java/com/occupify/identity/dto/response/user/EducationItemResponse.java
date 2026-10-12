package com.occupify.identity.dto.response.user;

import org.springframework.lang.Nullable;
import java.time.LocalDate;
import java.util.UUID;

public record EducationItemResponse(
        UUID id,
        String institution,
        String fieldOfStudy,
        @Nullable String degree,
        @Nullable String grade,
        @Nullable String description,
        LocalDate startDate,
        @Nullable LocalDate endDate
) {
}
