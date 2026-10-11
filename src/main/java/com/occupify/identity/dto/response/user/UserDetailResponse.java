package com.occupify.identity.dto.response.user;

import java.util.List;

public record UserDetailResponse(
        UserProfileResponse user,
        List<ExperienceItemResponse> experiences,
        List<EducationItemResponse> educations,
        List<QualificationItemResponse> qualifications,
        List<UserSkillItemResponse> skills
) {
}
