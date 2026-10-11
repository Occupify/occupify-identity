package com.occupify.identity.mapper;

import com.occupify.identity.dto.response.profile.SkillResponse;
import com.occupify.identity.dto.response.user.EducationItemResponse;
import com.occupify.identity.dto.response.user.ExperienceItemResponse;
import com.occupify.identity.dto.response.user.QualificationItemResponse;
import com.occupify.identity.dto.response.user.UserSkillItemResponse;
import com.occupify.identity.entity.Education;
import com.occupify.identity.entity.Experience;
import com.occupify.identity.entity.Qualification;
import com.occupify.identity.entity.Skill;
import com.occupify.identity.entity.SkillUser;
import org.springframework.stereotype.Component;

@Component
public class UserProfileMapper {

    public ExperienceItemResponse toExperienceResponse(Experience entity) {
        if (entity == null) {
            return null;
        }
        return new ExperienceItemResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getOrganization(),
                entity.getEmploymentType(),
                entity.getLocation(),
                entity.getLocationType(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.isCurrent(),
                entity.getDescription()
        );
    }

    public EducationItemResponse toEducationResponse(Education entity) {
        if (entity == null) {
            return null;
        }
        return new EducationItemResponse(
                entity.getId(),
                entity.getInstitution(),
                entity.getFieldOfStudy(),
                entity.getDegree(),
                entity.getGrade(),
                entity.getDescription(),
                entity.getStartDate(),
                entity.getEndDate()
        );
    }

    public QualificationItemResponse toQualificationResponse(Qualification entity) {
        if (entity == null) {
            return null;
        }
        return new QualificationItemResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getOrganization(),
                entity.getIssueDate(),
                entity.getExpirationDate(),
                entity.getCredentialId(),
                entity.getCredentialUrl()
        );
    }

    public UserSkillItemResponse toUserSkillResponse(SkillUser entity) {
        if (entity == null) {
            return null;
        }
        Skill skill = entity.getSkill();
        return new UserSkillItemResponse(
                skill != null ? skill.getId() : (entity.getId() != null ? entity.getId().getSkillId() : null),
                skill != null ? skill.getName() : null,
                skill != null ? skill.getCategory() : null,
                entity.isTopSkill(),
                entity.getProficiencyLevel(),
                entity.getYearsOfExperience()
        );
    }

    public SkillResponse toSkillResponse(Skill entity) {
        if (entity == null) {
            return null;
        }
        return new SkillResponse(
                entity.getId(),
                entity.getName(),
                entity.getCategory()
        );
    }
}
