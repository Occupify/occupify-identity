package com.occupify.identity.service.profile.impl;

import com.occupify.identity.dto.request.profile.AddUserSkillRequest;
import com.occupify.identity.dto.request.profile.CreateEducationRequest;
import com.occupify.identity.dto.request.profile.CreateExperienceRequest;
import com.occupify.identity.dto.request.profile.CreateQualificationRequest;
import com.occupify.identity.dto.request.profile.UpdateEducationRequest;
import com.occupify.identity.dto.request.profile.UpdateExperienceRequest;
import com.occupify.identity.dto.request.profile.UpdateQualificationRequest;
import com.occupify.identity.dto.request.profile.UpdateUserSkillRequest;
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
import com.occupify.identity.entity.User;
import com.occupify.identity.enums.UserStatus;
import com.occupify.identity.exception.profile.ProfileErrorCode;
import com.occupify.identity.exception.profile.ProfileException;
import com.occupify.identity.exception.user.UserErrorCode;
import com.occupify.identity.exception.user.UserException;
import com.occupify.identity.mapper.UserProfileMapper;
import com.occupify.identity.repository.EducationRepository;
import com.occupify.identity.repository.ExperienceRepository;
import com.occupify.identity.repository.QualificationRepository;
import com.occupify.identity.repository.SkillRepository;
import com.occupify.identity.repository.SkillUserRepository;
import com.occupify.identity.repository.UserRepository;
import com.occupify.identity.service.profile.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserProfileServiceImpl implements UserProfileService {

    private final UserRepository userRepository;
    private final ExperienceRepository experienceRepository;
    private final EducationRepository educationRepository;
    private final QualificationRepository qualificationRepository;
    private final SkillRepository skillRepository;
    private final SkillUserRepository skillUserRepository;
    private final UserProfileMapper userProfileMapper;

    // =========================================================================
    // EXPERIENCES
    // =========================================================================

    @Override
    @Transactional
    public ExperienceItemResponse addExperience(UUID currentUserId, CreateExperienceRequest request) {
        log.info("Adding experience for userId: {}", currentUserId);
        User user = findActiveUserOrThrow(currentUserId);

        if (request.endDate() != null && request.endDate().isBefore(request.startDate())) {
            throw new ProfileException(ProfileErrorCode.INVALID_DATE_RANGE, "End date cannot be earlier than start date");
        }

        Experience experience = new Experience(
                UUID.randomUUID(),
                user,
                request.title(),
                request.organization(),
                request.employmentType() != null ? request.employmentType().name() : null,
                request.location(),
                request.locationType() != null ? request.locationType().name() : null,
                request.startDate(),
                request.endDate(),
                request.isCurrent(),
                request.description()
        );

        Experience saved = experienceRepository.save(experience);
        log.info("Added experience with id: {} for userId: {}", saved.getId(), currentUserId);
        return userProfileMapper.toExperienceResponse(saved);
    }

    @Override
    @Transactional
    public ExperienceItemResponse updateExperience(UUID currentUserId, UUID experienceId, UpdateExperienceRequest request) {
        log.info("Updating experience id: {} for userId: {}", experienceId, currentUserId);
        Experience experience = experienceRepository.findByIdAndUserId(experienceId, currentUserId)
                .orElseThrow(() -> new ProfileException(ProfileErrorCode.EXPERIENCE_NOT_FOUND, "Experience record not found"));

        if (request.endDate() != null && request.endDate().isBefore(request.startDate())) {
            throw new ProfileException(ProfileErrorCode.INVALID_DATE_RANGE, "End date cannot be earlier than start date");
        }

        experience.setTitle(request.title());
        experience.setOrganization(request.organization());
        experience.setEmploymentType(request.employmentType() != null ? request.employmentType().name() : null);
        experience.setLocation(request.location());
        experience.setLocationType(request.locationType() != null ? request.locationType().name() : null);
        experience.setStartDate(request.startDate());
        experience.setEndDate(request.endDate());
        experience.setCurrent(request.isCurrent());
        experience.setDescription(request.description());

        Experience updated = experienceRepository.save(experience);
        log.info("Updated experience id: {} for userId: {}", experienceId, currentUserId);
        return userProfileMapper.toExperienceResponse(updated);
    }

    @Override
    @Transactional
    public void deleteExperience(UUID currentUserId, UUID experienceId) {
        log.warn("Deleting experience id: {} for userId: {}", experienceId, currentUserId);
        Experience experience = experienceRepository.findByIdAndUserId(experienceId, currentUserId)
                .orElseThrow(() -> new ProfileException(ProfileErrorCode.EXPERIENCE_NOT_FOUND, "Experience record not found"));

        experienceRepository.delete(experience);
        log.info("Deleted experience id: {} for userId: {}", experienceId, currentUserId);
    }

    // =========================================================================
    // EDUCATIONS
    // =========================================================================

    @Override
    @Transactional
    public EducationItemResponse addEducation(UUID currentUserId, CreateEducationRequest request) {
        log.info("Adding education for userId: {}", currentUserId);
        User user = findActiveUserOrThrow(currentUserId);

        if (request.endDate() != null && request.endDate().isBefore(request.startDate())) {
            throw new ProfileException(ProfileErrorCode.INVALID_DATE_RANGE, "End date cannot be earlier than start date");
        }

        Education education = new Education(
                UUID.randomUUID(),
                user,
                request.institution(),
                request.fieldOfStudy(),
                request.degree(),
                request.grade(),
                request.description(),
                request.startDate(),
                request.endDate()
        );

        Education saved = educationRepository.save(education);
        log.info("Added education with id: {} for userId: {}", saved.getId(), currentUserId);
        return userProfileMapper.toEducationResponse(saved);
    }

    @Override
    @Transactional
    public EducationItemResponse updateEducation(UUID currentUserId, UUID educationId, UpdateEducationRequest request) {
        log.info("Updating education id: {} for userId: {}", educationId, currentUserId);
        Education education = educationRepository.findByIdAndUserId(educationId, currentUserId)
                .orElseThrow(() -> new ProfileException(ProfileErrorCode.EDUCATION_NOT_FOUND, "Education record not found"));

        if (request.endDate() != null && request.endDate().isBefore(request.startDate())) {
            throw new ProfileException(ProfileErrorCode.INVALID_DATE_RANGE, "End date cannot be earlier than start date");
        }

        education.setInstitution(request.institution());
        education.setFieldOfStudy(request.fieldOfStudy());
        education.setDegree(request.degree());
        education.setGrade(request.grade());
        education.setDescription(request.description());
        education.setStartDate(request.startDate());
        education.setEndDate(request.endDate());

        Education updated = educationRepository.save(education);
        log.info("Updated education id: {} for userId: {}", educationId, currentUserId);
        return userProfileMapper.toEducationResponse(updated);
    }

    @Override
    @Transactional
    public void deleteEducation(UUID currentUserId, UUID educationId) {
        log.warn("Deleting education id: {} for userId: {}", educationId, currentUserId);
        Education education = educationRepository.findByIdAndUserId(educationId, currentUserId)
                .orElseThrow(() -> new ProfileException(ProfileErrorCode.EDUCATION_NOT_FOUND, "Education record not found"));

        educationRepository.delete(education);
        log.info("Deleted education id: {} for userId: {}", educationId, currentUserId);
    }

    // =========================================================================
    // QUALIFICATIONS
    // =========================================================================

    @Override
    @Transactional
    public QualificationItemResponse addQualification(UUID currentUserId, CreateQualificationRequest request) {
        log.info("Adding qualification for userId: {}", currentUserId);
        User user = findActiveUserOrThrow(currentUserId);

        if (request.expirationDate() != null && request.expirationDate().isBefore(request.issueDate())) {
            throw new ProfileException(ProfileErrorCode.INVALID_DATE_RANGE, "Expiration date cannot be earlier than issue date");
        }

        Qualification qualification = new Qualification(
                UUID.randomUUID(),
                user,
                request.title(),
                request.organization(),
                request.issueDate(),
                request.expirationDate(),
                request.credentialId(),
                request.credentialUrl()
        );

        Qualification saved = qualificationRepository.save(qualification);
        log.info("Added qualification with id: {} for userId: {}", saved.getId(), currentUserId);
        return userProfileMapper.toQualificationResponse(saved);
    }

    @Override
    @Transactional
    public QualificationItemResponse updateQualification(UUID currentUserId, UUID qualificationId, UpdateQualificationRequest request) {
        log.info("Updating qualification id: {} for userId: {}", qualificationId, currentUserId);
        Qualification qualification = qualificationRepository.findByIdAndUserId(qualificationId, currentUserId)
                .orElseThrow(() -> new ProfileException(ProfileErrorCode.QUALIFICATION_NOT_FOUND, "Qualification record not found"));

        if (request.expirationDate() != null && request.expirationDate().isBefore(request.issueDate())) {
            throw new ProfileException(ProfileErrorCode.INVALID_DATE_RANGE, "Expiration date cannot be earlier than issue date");
        }

        qualification.setTitle(request.title());
        qualification.setOrganization(request.organization());
        qualification.setIssueDate(request.issueDate());
        qualification.setExpirationDate(request.expirationDate());
        qualification.setCredentialId(request.credentialId());
        qualification.setCredentialUrl(request.credentialUrl());

        Qualification updated = qualificationRepository.save(qualification);
        log.info("Updated qualification id: {} for userId: {}", qualificationId, currentUserId);
        return userProfileMapper.toQualificationResponse(updated);
    }

    @Override
    @Transactional
    public void deleteQualification(UUID currentUserId, UUID qualificationId) {
        log.warn("Deleting qualification id: {} for userId: {}", qualificationId, currentUserId);
        Qualification qualification = qualificationRepository.findByIdAndUserId(qualificationId, currentUserId)
                .orElseThrow(() -> new ProfileException(ProfileErrorCode.QUALIFICATION_NOT_FOUND, "Qualification record not found"));

        qualificationRepository.delete(qualification);
        log.info("Deleted qualification id: {} for userId: {}", qualificationId, currentUserId);
    }

    // =========================================================================
    // SKILLS
    // =========================================================================

    @Override
    @Transactional
    public UserSkillItemResponse addSkill(UUID currentUserId, AddUserSkillRequest request) {
        log.info("Adding skill to profile for userId: {}", currentUserId);
        User user = findActiveUserOrThrow(currentUserId);

        Skill skill = resolveSkill(request);

        if (skillUserRepository.existsByIdUserIdAndIdSkillId(currentUserId, skill.getId())) {
            throw new ProfileException(ProfileErrorCode.SKILL_ALREADY_EXISTS_ON_PROFILE, "Skill is already attached to this profile");
        }

        SkillUser skillUser = new SkillUser(
                user,
                skill,
                request.isTopSkill(),
                request.proficiencyLevel() != null ? request.proficiencyLevel().name() : null,
                request.yearsOfExperience()
        );

        SkillUser saved = skillUserRepository.save(skillUser);
        log.info("Attached skill id: {} to userId: {}", skill.getId(), currentUserId);
        return userProfileMapper.toUserSkillResponse(saved);
    }

    @Override
    @Transactional
    public UserSkillItemResponse updateSkill(UUID currentUserId, UUID skillId, UpdateUserSkillRequest request) {
        log.info("Updating user skill id: {} for userId: {}", skillId, currentUserId);
        SkillUser skillUser = skillUserRepository.findByIdUserIdAndIdSkillId(currentUserId, skillId)
                .orElseThrow(() -> new ProfileException(ProfileErrorCode.USER_SKILL_NOT_FOUND, "Skill not found on user profile"));

        skillUser.setTopSkill(request.isTopSkill());
        skillUser.setProficiencyLevel(request.proficiencyLevel() != null ? request.proficiencyLevel().name() : null);
        skillUser.setYearsOfExperience(request.yearsOfExperience());

        SkillUser updated = skillUserRepository.save(skillUser);
        log.info("Updated user skill id: {} for userId: {}", skillId, currentUserId);
        return userProfileMapper.toUserSkillResponse(updated);
    }

    @Override
    @Transactional
    public void removeSkill(UUID currentUserId, UUID skillId) {
        log.warn("Removing skill id: {} for userId: {}", skillId, currentUserId);
        SkillUser skillUser = skillUserRepository.findByIdUserIdAndIdSkillId(currentUserId, skillId)
                .orElseThrow(() -> new ProfileException(ProfileErrorCode.USER_SKILL_NOT_FOUND, "Skill not found on user profile"));

        skillUserRepository.delete(skillUser);
        log.info("Removed skill id: {} from userId: {}", skillId, currentUserId);
    }

    @Override
    public List<SkillResponse> searchSkills(String keyword) {
        log.debug("Searching skills catalog with keyword: {}", keyword);
        List<Skill> skills;
        if (keyword == null || keyword.isBlank()) {
            skills = skillRepository.findAll();
        } else {
            skills = skillRepository.findByNameContainingIgnoreCase(keyword.trim());
        }
        return skills.stream()
                .map(userProfileMapper::toSkillResponse)
                .toList();
    }

    private User findActiveUserOrThrow(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND, "User not found with id: " + userId));

        if (UserStatus.DELETED.name().equalsIgnoreCase(user.getStatus())) {
            throw new UserException(UserErrorCode.USER_ALREADY_DELETED, "User account has been deleted");
        }
        return user;
    }

    private Skill resolveSkill(AddUserSkillRequest request) {
        if (request.skillId() != null) {
            return skillRepository.findById(request.skillId())
                    .orElseThrow(() -> new ProfileException(ProfileErrorCode.SKILL_NOT_FOUND, "Skill not found with id: " + request.skillId()));
        }

        if (request.skillName() != null && !request.skillName().isBlank()) {
            String name = request.skillName().trim();
            return skillRepository.findByNameIgnoreCase(name)
                    .orElseGet(() -> skillRepository.save(new Skill(UUID.randomUUID(), name, request.category())));
        }

        throw new ProfileException(ProfileErrorCode.SKILL_NOT_FOUND, "Skill id or skill name must be provided");
    }
}
