package com.occupify.identity.dto.response.profile;

import java.util.UUID;

public record SkillResponse(
        UUID id,
        String name,
        String category
) {
}
