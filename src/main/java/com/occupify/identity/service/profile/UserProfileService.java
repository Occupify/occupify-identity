package com.occupify.identity.service.profile;

import com.occupify.identity.dto.request.profile.AddUserSkillRequest;
import com.occupify.identity.dto.request.profile.CreateEducationRequest;
import com.occupify.identity.dto.request.profile.CreateExperienceRequest;
import com.occupify.identity.dto.request.profile.CreateQualificationRequest;
import com.occupify.identity.dto.request.profile.UpdateEducationRequest;
import com.occupify.identity.dto.request.profile.UpdateExperienceRequest;
import com.occupify.identity.dto.request.profile.UpdateQualificationRequest;
import com.occupify.identity.dto.request.profile.UpdateUserSkillRequest;
import com.occupify.identity.dto.response.profile.InstitutionResponse;
import com.occupify.identity.dto.response.profile.SkillResponse;
import com.occupify.identity.dto.response.user.EducationItemResponse;
import com.occupify.identity.dto.response.user.ExperienceItemResponse;
import com.occupify.identity.dto.response.user.QualificationItemResponse;
import com.occupify.identity.dto.response.user.UserSkillItemResponse;

import java.util.List;
import java.util.UUID;

public interface UserProfileService {
    ExperienceItemResponse addExperience(UUID currentUserId, CreateExperienceRequest request);
    ExperienceItemResponse updateExperience(UUID currentUserId, UUID experienceId, UpdateExperienceRequest request);
    void deleteExperience(UUID currentUserId, UUID experienceId);

    EducationItemResponse addEducation(UUID currentUserId, CreateEducationRequest request);
    EducationItemResponse updateEducation(UUID currentUserId, UUID educationId, UpdateEducationRequest request);
    void deleteEducation(UUID currentUserId, UUID educationId);

    QualificationItemResponse addQualification(UUID currentUserId, CreateQualificationRequest request);
    QualificationItemResponse updateQualification(UUID currentUserId, UUID qualificationId, UpdateQualificationRequest request);
    void deleteQualification(UUID currentUserId, UUID qualificationId);

    UserSkillItemResponse addSkill(UUID currentUserId, AddUserSkillRequest request);
    UserSkillItemResponse updateSkill(UUID currentUserId, UUID skillId, UpdateUserSkillRequest request);
    void removeSkill(UUID currentUserId, UUID skillId);
    List<SkillResponse> searchSkills(String keyword);

    List<InstitutionResponse> searchInstitutions(String keyword);
}
