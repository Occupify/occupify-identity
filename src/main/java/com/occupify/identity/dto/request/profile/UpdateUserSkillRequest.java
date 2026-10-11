package com.occupify.identity.dto.request.profile;

import com.occupify.identity.enums.ProficiencyLevel;

public record UpdateUserSkillRequest(
        boolean isTopSkill,
        ProficiencyLevel proficiencyLevel,
        Integer yearsOfExperience
) {
}
