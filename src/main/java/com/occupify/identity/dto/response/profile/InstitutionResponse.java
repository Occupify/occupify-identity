package com.occupify.identity.dto.response.profile;

import org.springframework.lang.Nullable;
import java.util.UUID;

public record InstitutionResponse(
        UUID id,
        String name,
        @Nullable String code,
        @Nullable String country,
        @Nullable String city,
        @Nullable String website,
        @Nullable String logoUrl
) {
}
