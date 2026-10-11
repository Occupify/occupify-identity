package com.occupify.identity.dto.response.user;

import java.util.UUID;

public record UserSkillItemResponse(
        UUID skillId,
        String skillName,
        String category,
        boolean isTopSkill,
        String proficiencyLevel,
        Integer yearsOfExperience
) {
}
