package com.occupify.identity.dto.response.user;

import org.springframework.lang.Nullable;
import java.time.LocalDate;
import java.util.UUID;

public record QualificationItemResponse(
        UUID id,
        String title,
        String organization,
        LocalDate issueDate,
        @Nullable LocalDate expirationDate,
        @Nullable String credentialId,
        @Nullable String credentialUrl,
        @Nullable String documentUrl
) {
}
