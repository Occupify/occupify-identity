package com.occupify.identity.dto.response.user;

import java.time.LocalDate;
import java.util.UUID;

public record QualificationItemResponse(
        UUID id,
        String title,
        String organization,
        LocalDate issueDate,
        LocalDate expirationDate,
        String credentialId,
        String credentialUrl
) {
}
