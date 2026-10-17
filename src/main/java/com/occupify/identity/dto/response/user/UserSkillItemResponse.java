package com.occupify.identity.dto.response.user;

import org.springframework.lang.Nullable;
import java.util.UUID;

public record UserSkillItemResponse(
        @Nullable UUID skillId,
        @Nullable String skillName,
        @Nullable String category,
        boolean isTopSkill,
        @Nullable String proficiencyLevel,
        @Nullable Integer yearsOfExperience
) {
}
