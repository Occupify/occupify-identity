package com.occupify.identity.dto.response.user;

import java.time.LocalDate;
import java.util.UUID;

public record EducationItemResponse(
        UUID id,
        String institution,
        String fieldOfStudy,
        String degree,
        String grade,
        String description,
        LocalDate startDate,
        LocalDate endDate
) {
}
