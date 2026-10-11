package com.occupify.identity.dto.request.profile;

import com.occupify.identity.enums.ProficiencyLevel;

import java.util.UUID;

public record AddUserSkillRequest(
        UUID skillId,
        String skillName,
        String category,
        boolean isTopSkill,
        ProficiencyLevel proficiencyLevel,
        Integer yearsOfExperience
) {
}
