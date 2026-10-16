package com.occupify.identity.service.profile;

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
import com.occupify.identity.enums.EmploymentType;
import com.occupify.identity.enums.LocationType;
import com.occupify.identity.enums.ProficiencyLevel;
import com.occupify.identity.enums.UserRole;
import com.occupify.identity.enums.UserStatus;
import com.occupify.identity.exception.profile.ProfileErrorCode;
import com.occupify.identity.exception.profile.ProfileException;
import com.occupify.identity.mapper.UserProfileMapper;
import com.occupify.identity.repository.EducationRepository;
import com.occupify.identity.repository.ExperienceRepository;
import com.occupify.identity.repository.QualificationRepository;
import com.occupify.identity.repository.SkillRepository;
import com.occupify.identity.repository.SkillUserRepository;
import com.occupify.identity.repository.UserRepository;
import com.occupify.identity.service.profile.impl.UserProfileServiceImpl;
import com.occupify.identity.dto.response.profile.InstitutionResponse;
import com.occupify.identity.entity.Institution;
import com.occupify.identity.repository.InstitutionRepository;
import com.occupify.identity.service.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserProfileServiceImpl Unit Tests")
class UserProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ExperienceRepository experienceRepository;

    @Mock
    private EducationRepository educationRepository;

    @Mock
    private QualificationRepository qualificationRepository;

    @Mock
    private SkillRepository skillRepository;

    @Mock
    private SkillUserRepository skillUserRepository;

    @Mock
    private InstitutionRepository institutionRepository;

    @Mock
    private StorageService storageService;

    @Spy
    private UserProfileMapper userProfileMapper = new UserProfileMapper();

    @InjectMocks
    private UserProfileServiceImpl userProfileService;

    private UUID userId;
    private User activeUser;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        activeUser = new User(
                userId,
                "freelancer@occupify.work",
                "hashed",
                UserRole.USER.name(),
                UserStatus.ACTIVE.name(),
                Instant.now(),
                Instant.now()
        );
        activeUser.setFullName("Dev User");
        activeUser.setHeadline("Engineer");
        activeUser.setAvatarUrl("https://avatar.url");
        activeUser.setBio("Bio");
        activeUser.setPhoneNumber("+84901234567");
    }

    // =========================================================================
    // EXPERIENCES
    // =========================================================================

    @Test
    @DisplayName("addExperience -> successfully saves and returns ExperienceItemResponse")
    void addExperience_success() {
        CreateExperienceRequest request = new CreateExperienceRequest(
                "Backend Engineer",
                "Google",
                EmploymentType.FULL_TIME,
                "Mountain View",
                LocationType.HYBRID,
                LocalDate.of(2021, 1, 1),
                LocalDate.of(2023, 1, 1),
                false,
                "Engineered distributed infrastructure"
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(experienceRepository.save(any(Experience.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExperienceItemResponse response = userProfileService.addExperience(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo("Backend Engineer");
        assertThat(response.organization()).isEqualTo("Google");
        verify(experienceRepository).save(any(Experience.class));
    }

    @Test
    @DisplayName("addExperience -> throws INVALID_DATE_RANGE when endDate < startDate")
    void addExperience_invalidDates_throwsProfileException() {
        CreateExperienceRequest request = new CreateExperienceRequest(
                "Backend Engineer",
                "Google",
                EmploymentType.FULL_TIME,
                "Mountain View",
                LocationType.HYBRID,
                LocalDate.of(2023, 1, 1),
                LocalDate.of(2020, 1, 1), // Invalid endDate
                false,
                "Summary"
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        assertThatThrownBy(() -> userProfileService.addExperience(userId, request))
                .isInstanceOf(ProfileException.class)
                .extracting("errorCode")
                .isEqualTo(ProfileErrorCode.INVALID_DATE_RANGE);
    }

    @Test
    @DisplayName("updateExperience -> successfully updates existing experience")
    void updateExperience_success() {
        UUID expId = UUID.randomUUID();
        Experience existing = new Experience(
                expId,
                activeUser,
                "Old Title",
                "Old Org",
                EmploymentType.FULL_TIME.name(),
                "Remote",
                LocationType.REMOTE.name(),
                LocalDate.of(2020, 1, 1),
                null,
                true,
                "Old desc"
        );

        UpdateExperienceRequest updateRequest = new UpdateExperienceRequest(
                "New Title",
                "New Org",
                EmploymentType.CONTRACT,
                "Remote",
                LocationType.REMOTE,
                LocalDate.of(2020, 1, 1),
                null,
                true,
                "New desc"
        );

        when(experienceRepository.findByIdAndUserId(expId, userId)).thenReturn(Optional.of(existing));
        when(experienceRepository.save(existing)).thenReturn(existing);

        ExperienceItemResponse response = userProfileService.updateExperience(userId, expId, updateRequest);

        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo("New Title");
        assertThat(response.organization()).isEqualTo("New Org");
    }

    @Test
    @DisplayName("deleteExperience -> removes experience when found")
    void deleteExperience_success() {
        UUID expId = UUID.randomUUID();
        Experience existing = new Experience(
                expId,
                activeUser,
                "Title",
                "Org",
                EmploymentType.FULL_TIME.name(),
                "Remote",
                LocationType.REMOTE.name(),
                LocalDate.of(2020, 1, 1),
                null,
                true,
                "desc"
        );

        when(experienceRepository.findByIdAndUserId(expId, userId)).thenReturn(Optional.of(existing));

        userProfileService.deleteExperience(userId, expId);

        verify(experienceRepository).delete(existing);
    }

    // =========================================================================
    // EDUCATIONS
    // =========================================================================

    @Test
    @DisplayName("addEducation -> saves and returns EducationItemResponse with institution relation")
    void addEducation_success() {
        UUID instId = UUID.randomUUID();
        Institution institution = new Institution(instId, "MIT", "MIT", "USA", "Cambridge", "https://mit.edu", null, true);

        CreateEducationRequest request = new CreateEducationRequest(
                instId,
                "Computer Science",
                "Bachelor of Science",
                "4.0",
                "Graduated top of class",
                LocalDate.of(2016, 9, 1),
                LocalDate.of(2020, 6, 1),
                null
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(institutionRepository.findById(instId)).thenReturn(Optional.of(institution));
        when(educationRepository.save(any(Education.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EducationItemResponse response = userProfileService.addEducation(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.institutionId()).isEqualTo(instId);
        assertThat(response.institutionName()).isEqualTo("MIT");
        assertThat(response.fieldOfStudy()).isEqualTo("Computer Science");
        assertThat(response.documentUrl()).isNull();
    }

    @Test
    @DisplayName("addEducation -> with document file uploads to storage and stores documentUrl")
    void addEducation_withDocument_uploadsToStorage() {
        UUID instId = UUID.randomUUID();
        Institution institution = new Institution(instId, "MIT", "MIT", "USA", "Cambridge", "https://mit.edu", null, true);
        MockMultipartFile docFile = new MockMultipartFile("document", "diploma.pdf", "application/pdf", "dummy pdf".getBytes());

        CreateEducationRequest request = new CreateEducationRequest(
                instId,
                "Computer Science",
                "Bachelor of Science",
                "4.0",
                "Graduated top of class",
                LocalDate.of(2016, 9, 1),
                LocalDate.of(2020, 6, 1),
                docFile
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(institutionRepository.findById(instId)).thenReturn(Optional.of(institution));
        when(storageService.uploadDocument(eq("educations/" + userId), eq(docFile)))
                .thenReturn("https://s3.amazonaws.com/bucket/educations/" + userId + "/doc.pdf");
        when(educationRepository.save(any(Education.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EducationItemResponse response = userProfileService.addEducation(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.documentUrl()).isEqualTo("https://s3.amazonaws.com/bucket/educations/" + userId + "/doc.pdf");
        verify(storageService).uploadDocument(eq("educations/" + userId), eq(docFile));
    }

    @Test
    @DisplayName("addEducation -> throws INSTITUTION_NOT_FOUND when institutionId does not exist")
    void addEducation_institutionNotFound_throwsException() {
        UUID instId = UUID.randomUUID();
        CreateEducationRequest request = new CreateEducationRequest(
                instId,
                "Computer Science",
                "Bachelor of Science",
                "4.0",
                "Desc",
                LocalDate.of(2016, 9, 1),
                LocalDate.of(2020, 6, 1),
                null
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(institutionRepository.findById(instId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userProfileService.addEducation(userId, request))
                .isInstanceOf(ProfileException.class)
                .extracting("errorCode")
                .isEqualTo(ProfileErrorCode.INSTITUTION_NOT_FOUND);

        verify(educationRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateEducation -> with new document deletes old document from storage and saves new URL")
    void updateEducation_withNewDocument_deletesOldDocumentAndUploadsNew() {
        UUID eduId = UUID.randomUUID();
        UUID instId = UUID.randomUUID();
        Institution institution = new Institution(instId, "MIT", "MIT", "USA", "Cambridge", "https://mit.edu", null, true);

        Education existing = new Education(
                eduId,
                activeUser,
                institution,
                "CS",
                "BSc",
                "4.0",
                "Desc",
                LocalDate.of(2016, 9, 1),
                LocalDate.of(2020, 6, 1),
                "https://s3.amazonaws.com/bucket/old-diploma.pdf"
        );

        MockMultipartFile newFile = new MockMultipartFile("document", "new-diploma.pdf", "application/pdf", "new content".getBytes());
        UpdateEducationRequest updateRequest = new UpdateEducationRequest(
                instId,
                "Advanced CS",
                "MSc",
                "4.0",
                "Updated",
                LocalDate.of(2016, 9, 1),
                LocalDate.of(2020, 6, 1),
                newFile,
                false
        );

        when(educationRepository.findByIdAndUserId(eduId, userId)).thenReturn(Optional.of(existing));
        when(institutionRepository.findById(instId)).thenReturn(Optional.of(institution));
        when(storageService.uploadDocument(eq("educations/" + userId), eq(newFile)))
                .thenReturn("https://s3.amazonaws.com/bucket/new-diploma.pdf");
        when(educationRepository.save(existing)).thenReturn(existing);

        EducationItemResponse response = userProfileService.updateEducation(userId, eduId, updateRequest);

        assertThat(response).isNotNull();
        assertThat(response.documentUrl()).isEqualTo("https://s3.amazonaws.com/bucket/new-diploma.pdf");
        verify(storageService).deleteFile("https://s3.amazonaws.com/bucket/old-diploma.pdf");
        verify(storageService).uploadDocument(eq("educations/" + userId), eq(newFile));
    }

    @Test
    @DisplayName("deleteEducation -> deletes education and removes proof document from storage")
    void deleteEducation_success() {
        UUID eduId = UUID.randomUUID();
        UUID instId = UUID.randomUUID();
        Institution institution = new Institution(instId, "MIT", "MIT", "USA", "Cambridge", "https://mit.edu", null, true);

        Education existing = new Education(
                eduId,
                activeUser,
                institution,
                "CS",
                "BSc",
                "4.0",
                "Desc",
                LocalDate.of(2016, 9, 1),
                LocalDate.of(2020, 6, 1),
                "https://s3.amazonaws.com/bucket/diploma.pdf"
        );

        when(educationRepository.findByIdAndUserId(eduId, userId)).thenReturn(Optional.of(existing));

        userProfileService.deleteEducation(userId, eduId);

        verify(storageService).deleteFile("https://s3.amazonaws.com/bucket/diploma.pdf");
        verify(educationRepository).delete(existing);
    }

    // =========================================================================
    // QUALIFICATIONS
    // =========================================================================

    @Test
    @DisplayName("addQualification -> saves and returns QualificationItemResponse")
    void addQualification_success() {
        CreateQualificationRequest request = new CreateQualificationRequest(
                "CKA: Certified Kubernetes Administrator",
                "CNCF",
                LocalDate.of(2023, 1, 1),
                LocalDate.of(2026, 1, 1),
                "CKA-12345",
                "https://cncf.io/verify",
                null
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(qualificationRepository.save(any(Qualification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        QualificationItemResponse response = userProfileService.addQualification(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo("CKA: Certified Kubernetes Administrator");
        assertThat(response.organization()).isEqualTo("CNCF");
        assertThat(response.documentUrl()).isNull();
    }

    @Test
    @DisplayName("addQualification -> with document uploads to storage")
    void addQualification_withDocument_uploadsToStorage() {
        MockMultipartFile docFile = new MockMultipartFile("document", "cka.pdf", "application/pdf", "dummy pdf".getBytes());
        CreateQualificationRequest request = new CreateQualificationRequest(
                "CKA: Certified Kubernetes Administrator",
                "CNCF",
                LocalDate.of(2023, 1, 1),
                LocalDate.of(2026, 1, 1),
                "CKA-12345",
                "https://cncf.io/verify",
                docFile
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(storageService.uploadDocument(eq("certificates/" + userId), eq(docFile)))
                .thenReturn("https://s3.amazonaws.com/bucket/certificates/" + userId + "/cka.pdf");
        when(qualificationRepository.save(any(Qualification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        QualificationItemResponse response = userProfileService.addQualification(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.documentUrl()).isEqualTo("https://s3.amazonaws.com/bucket/certificates/" + userId + "/cka.pdf");
        verify(storageService).uploadDocument(eq("certificates/" + userId), eq(docFile));
    }

    @Test
    @DisplayName("updateQualification -> with new document deletes old document from storage")
    void updateQualification_withNewDocument_deletesOldDocument() {
        UUID qualId = UUID.randomUUID();
        Qualification existing = new Qualification(
                qualId,
                activeUser,
                "CKA",
                "CNCF",
                LocalDate.of(2023, 1, 1),
                LocalDate.of(2026, 1, 1),
                "ID-123",
                "https://verify.url",
                "https://s3.amazonaws.com/bucket/old-cert.pdf"
        );

        MockMultipartFile newFile = new MockMultipartFile("document", "new-cert.pdf", "application/pdf", "content".getBytes());
        UpdateQualificationRequest request = new UpdateQualificationRequest(
                "CKA Updated",
                "CNCF",
                LocalDate.of(2023, 1, 1),
                LocalDate.of(2026, 1, 1),
                "ID-123",
                "https://verify.url",
                newFile,
                false
        );

        when(qualificationRepository.findByIdAndUserId(qualId, userId)).thenReturn(Optional.of(existing));
        when(storageService.uploadDocument(eq("certificates/" + userId), eq(newFile)))
                .thenReturn("https://s3.amazonaws.com/bucket/new-cert.pdf");
        when(qualificationRepository.save(existing)).thenReturn(existing);

        QualificationItemResponse response = userProfileService.updateQualification(userId, qualId, request);

        assertThat(response).isNotNull();
        assertThat(response.documentUrl()).isEqualTo("https://s3.amazonaws.com/bucket/new-cert.pdf");
        verify(storageService).deleteFile("https://s3.amazonaws.com/bucket/old-cert.pdf");
    }

    @Test
    @DisplayName("deleteQualification -> deletes qualification and removes proof document from storage")
    void deleteQualification_success() {
        UUID qualId = UUID.randomUUID();
        Qualification existing = new Qualification(
                qualId,
                activeUser,
                "CKA",
                "CNCF",
                LocalDate.of(2023, 1, 1),
                LocalDate.of(2026, 1, 1),
                "ID-123",
                "https://verify.url",
                "https://s3.amazonaws.com/bucket/cert.pdf"
        );

        when(qualificationRepository.findByIdAndUserId(qualId, userId)).thenReturn(Optional.of(existing));

        userProfileService.deleteQualification(userId, qualId);

        verify(storageService).deleteFile("https://s3.amazonaws.com/bucket/cert.pdf");
        verify(qualificationRepository).delete(existing);
    }

    // =========================================================================
    // SKILLS
    // =========================================================================

    @Test
    @DisplayName("addSkill -> attaches skill to profile using existing skillId")
    void addSkill_existingSkillId_success() {
        UUID skillId = UUID.randomUUID();
        Skill skill = new Skill(skillId, "Spring Boot", "Frameworks");
        AddUserSkillRequest request = new AddUserSkillRequest(
                skillId,
                "Spring Boot",
                "Frameworks",
                true,
                ProficiencyLevel.EXPERT,
                5
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(skillRepository.findById(skillId)).thenReturn(Optional.of(skill));
        when(skillUserRepository.existsByIdUserIdAndIdSkillId(userId, skillId)).thenReturn(false);
        when(skillUserRepository.save(any(SkillUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserSkillItemResponse response = userProfileService.addSkill(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.skillId()).isEqualTo(skillId);
        assertThat(response.skillName()).isEqualTo("Spring Boot");
        assertThat(response.isTopSkill()).isTrue();
    }

    @Test
    @DisplayName("addSkill -> creates new skill in catalog if not existing and attaches to profile")
    void addSkill_newSkillName_success() {
        AddUserSkillRequest request = new AddUserSkillRequest(
                null,
                "Rust",
                "Programming Languages",
                false,
                ProficiencyLevel.INTERMEDIATE,
                2
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(skillRepository.findByNameIgnoreCase("Rust")).thenReturn(Optional.empty());
        when(skillRepository.save(any(Skill.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(skillUserRepository.existsByIdUserIdAndIdSkillId(eq(userId), any())).thenReturn(false);
        when(skillUserRepository.save(any(SkillUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserSkillItemResponse response = userProfileService.addSkill(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.skillName()).isEqualTo("Rust");
    }

    @Test
    @DisplayName("addSkill -> throws SKILL_ALREADY_EXISTS_ON_PROFILE if already attached")
    void addSkill_duplicate_throwsProfileException() {
        UUID skillId = UUID.randomUUID();
        Skill skill = new Skill(skillId, "Java", "Languages");
        AddUserSkillRequest request = new AddUserSkillRequest(
                skillId,
                "Java",
                "Languages",
                true,
                ProficiencyLevel.EXPERT,
                8
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(skillRepository.findById(skillId)).thenReturn(Optional.of(skill));
        when(skillUserRepository.existsByIdUserIdAndIdSkillId(userId, skillId)).thenReturn(true);

        assertThatThrownBy(() -> userProfileService.addSkill(userId, request))
                .isInstanceOf(ProfileException.class)
                .extracting("errorCode")
                .isEqualTo(ProfileErrorCode.SKILL_ALREADY_EXISTS_ON_PROFILE);
    }

    @Test
    @DisplayName("updateSkill -> mutates proficiency level and top skill flag")
    void updateSkill_success() {
        UUID skillId = UUID.randomUUID();
        Skill skill = new Skill(skillId, "Docker", "DevOps");
        SkillUser skillUser = new SkillUser(activeUser, skill, false, "BEGINNER", 1);

        UpdateUserSkillRequest request = new UpdateUserSkillRequest(
                true,
                ProficiencyLevel.ADVANCED,
                4
        );

        when(skillUserRepository.findByIdUserIdAndIdSkillId(userId, skillId)).thenReturn(Optional.of(skillUser));
        when(skillUserRepository.save(skillUser)).thenReturn(skillUser);

        UserSkillItemResponse response = userProfileService.updateSkill(userId, skillId, request);

        assertThat(response).isNotNull();
        assertThat(response.isTopSkill()).isTrue();
        assertThat(response.proficiencyLevel()).isEqualTo(ProficiencyLevel.ADVANCED.name());
        assertThat(response.yearsOfExperience()).isEqualTo(4);
    }

    @Test
    @DisplayName("removeSkill -> deletes skillUser association")
    void removeSkill_success() {
        UUID skillId = UUID.randomUUID();
        Skill skill = new Skill(skillId, "Docker", "DevOps");
        SkillUser skillUser = new SkillUser(activeUser, skill, false, "BEGINNER", 1);

        when(skillUserRepository.findByIdUserIdAndIdSkillId(userId, skillId)).thenReturn(Optional.of(skillUser));

        userProfileService.removeSkill(userId, skillId);

        verify(skillUserRepository).delete(skillUser);
    }

    @Test
    @DisplayName("searchSkills -> returns matching skills from catalog")
    void searchSkills_success() {
        List<Skill> skills = List.of(
                new Skill(UUID.randomUUID(), "Spring Boot", "Frameworks"),
                new Skill(UUID.randomUUID(), "Spring Security", "Frameworks")
        );

        when(skillRepository.findByNameContainingIgnoreCase("spring")).thenReturn(skills);

        List<SkillResponse> responses = userProfileService.searchSkills("spring");

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).name()).isEqualTo("Spring Boot");
    }

    @Test
    @DisplayName("searchInstitutions -> returns matching institutions from catalog")
    void searchInstitutions_success() {
        List<Institution> institutions = List.of(
                new Institution(UUID.randomUUID(), "HUST", "HUST", "Vietnam", "Hanoi", "https://hust.edu.vn", null, true),
                new Institution(UUID.randomUUID(), "HCMUT", "HCMUT", "Vietnam", "Ho Chi Minh City", "https://hcmut.edu.vn", null, true)
        );

        when(institutionRepository.findByNameContainingIgnoreCaseOrderByNameAsc("hust")).thenReturn(List.of(institutions.get(0)));

        List<InstitutionResponse> responses = userProfileService.searchInstitutions("hust");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).name()).isEqualTo("HUST");
    }
}
