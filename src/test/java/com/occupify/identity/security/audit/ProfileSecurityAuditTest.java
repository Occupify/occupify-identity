package com.occupify.identity.security.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.occupify.identity.dto.request.profile.UpdateEducationRequest;
import com.occupify.identity.dto.request.profile.UpdateExperienceRequest;
import com.occupify.identity.dto.request.profile.UpdateQualificationRequest;
import com.occupify.identity.dto.request.profile.UpdateUserSkillRequest;
import com.occupify.identity.dto.response.user.UserDetailResponse;
import com.occupify.identity.dto.response.user.UserProfileResponse;
import com.occupify.identity.entity.User;
import com.occupify.identity.enums.EmploymentType;
import com.occupify.identity.enums.LocationType;
import com.occupify.identity.enums.ProficiencyLevel;
import com.occupify.identity.enums.UserRole;
import com.occupify.identity.enums.UserStatus;
import com.occupify.identity.exception.profile.ProfileErrorCode;
import com.occupify.identity.exception.profile.ProfileException;
import com.occupify.identity.mapper.UserMapper;
import com.occupify.identity.mapper.UserProfileMapper;
import com.occupify.identity.repository.EducationRepository;
import com.occupify.identity.repository.ExperienceRepository;
import com.occupify.identity.repository.QualificationRepository;
import com.occupify.identity.repository.SkillRepository;
import com.occupify.identity.repository.SkillUserRepository;
import com.occupify.identity.repository.UserRepository;
import com.occupify.identity.service.profile.impl.UserProfileServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Security Audit: IDOR Defense & Sensitive Data Leakage Audit")
class ProfileSecurityAuditTest {

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

    @Spy
    private UserProfileMapper userProfileMapper = new UserProfileMapper();

    @Spy
    private UserMapper userMapper = new UserMapper();

    @InjectMocks
    private UserProfileServiceImpl userProfileService;

    private ObjectMapper objectMapper;
    private UUID userAId;
    private UUID attackerUserBId;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        userAId = UUID.randomUUID();
        attackerUserBId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("1. IDOR Prevention Verification")
    class IdorPreventionTests {

        @Test
        @DisplayName("IDOR-01: Attacker B cannot update User A's experience")
        void attackerCannotUpdateVictimExperience() {
            UUID victimExperienceId = UUID.randomUUID();
            UpdateExperienceRequest maliciousUpdate = new UpdateExperienceRequest(
                    "Hacked Title",
                    "Hacked Org",
                    EmploymentType.FULL_TIME,
                    "Remote",
                    LocationType.REMOTE,
                    LocalDate.of(2022, 1, 1),
                    null,
                    true,
                    "Malicious modification"
            );

            // Attacker B attempts to update victim's experienceId: repository must query by (experienceId, attackerUserBId)
            when(experienceRepository.findByIdAndUserId(victimExperienceId, attackerUserBId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> userProfileService.updateExperience(attackerUserBId, victimExperienceId, maliciousUpdate))
                    .isInstanceOf(ProfileException.class)
                    .extracting("errorCode")
                    .isEqualTo(ProfileErrorCode.EXPERIENCE_NOT_FOUND);

            verify(experienceRepository, never()).save(any());
        }

        @Test
        @DisplayName("IDOR-02: Attacker B cannot delete User A's experience")
        void attackerCannotDeleteVictimExperience() {
            UUID victimExperienceId = UUID.randomUUID();

            when(experienceRepository.findByIdAndUserId(victimExperienceId, attackerUserBId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> userProfileService.deleteExperience(attackerUserBId, victimExperienceId))
                    .isInstanceOf(ProfileException.class)
                    .extracting("errorCode")
                    .isEqualTo(ProfileErrorCode.EXPERIENCE_NOT_FOUND);

            verify(experienceRepository, never()).delete(any());
        }

        @Test
        @DisplayName("IDOR-03: Attacker B cannot update User A's education")
        void attackerCannotUpdateVictimEducation() {
            UUID victimEducationId = UUID.randomUUID();
            UpdateEducationRequest maliciousUpdate = new UpdateEducationRequest(
                    UUID.randomUUID(),
                    "Fake Field",
                    "Fake Degree",
                    "4.0",
                    "Tampered",
                    LocalDate.of(2020, 1, 1),
                    LocalDate.of(2024, 1, 1),
                    null,
                    false
            );

            when(educationRepository.findByIdAndUserId(victimEducationId, attackerUserBId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> userProfileService.updateEducation(attackerUserBId, victimEducationId, maliciousUpdate))
                    .isInstanceOf(ProfileException.class)
                    .extracting("errorCode")
                    .isEqualTo(ProfileErrorCode.EDUCATION_NOT_FOUND);

            verify(educationRepository, never()).save(any());
        }

        @Test
        @DisplayName("IDOR-04: Attacker B cannot delete User A's education")
        void attackerCannotDeleteVictimEducation() {
            UUID victimEducationId = UUID.randomUUID();

            when(educationRepository.findByIdAndUserId(victimEducationId, attackerUserBId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> userProfileService.deleteEducation(attackerUserBId, victimEducationId))
                    .isInstanceOf(ProfileException.class)
                    .extracting("errorCode")
                    .isEqualTo(ProfileErrorCode.EDUCATION_NOT_FOUND);

            verify(educationRepository, never()).delete(any());
        }

        @Test
        @DisplayName("IDOR-05: Attacker B cannot update User A's qualification")
        void attackerCannotUpdateVictimQualification() {
            UUID victimQualificationId = UUID.randomUUID();
            UpdateQualificationRequest maliciousUpdate = new UpdateQualificationRequest(
                    "Tampered Cert",
                    "Tampered Issuer",
                    LocalDate.of(2023, 1, 1),
                    null,
                    "FAKE-ID",
                    "https://fake.url",
                    null,
                    false
            );

            when(qualificationRepository.findByIdAndUserId(victimQualificationId, attackerUserBId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> userProfileService.updateQualification(attackerUserBId, victimQualificationId, maliciousUpdate))
                    .isInstanceOf(ProfileException.class)
                    .extracting("errorCode")
                    .isEqualTo(ProfileErrorCode.QUALIFICATION_NOT_FOUND);

            verify(qualificationRepository, never()).save(any());
        }

        @Test
        @DisplayName("IDOR-06: Attacker B cannot delete User A's qualification")
        void attackerCannotDeleteVictimQualification() {
            UUID victimQualificationId = UUID.randomUUID();

            when(qualificationRepository.findByIdAndUserId(victimQualificationId, attackerUserBId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> userProfileService.deleteQualification(attackerUserBId, victimQualificationId))
                    .isInstanceOf(ProfileException.class)
                    .extracting("errorCode")
                    .isEqualTo(ProfileErrorCode.QUALIFICATION_NOT_FOUND);

            verify(qualificationRepository, never()).delete(any());
        }

        @Test
        @DisplayName("IDOR-07: Attacker B cannot update or remove User A's attached skill")
        void attackerCannotTamperVictimSkill() {
            UUID victimSkillId = UUID.randomUUID();
            UpdateUserSkillRequest maliciousUpdate = new UpdateUserSkillRequest(
                    true,
                    ProficiencyLevel.EXPERT,
                    10
            );

            when(skillUserRepository.findByIdUserIdAndIdSkillId(attackerUserBId, victimSkillId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> userProfileService.updateSkill(attackerUserBId, victimSkillId, maliciousUpdate))
                    .isInstanceOf(ProfileException.class)
                    .extracting("errorCode")
                    .isEqualTo(ProfileErrorCode.USER_SKILL_NOT_FOUND);

            assertThatThrownBy(() -> userProfileService.removeSkill(attackerUserBId, victimSkillId))
                    .isInstanceOf(ProfileException.class)
                    .extracting("errorCode")
                    .isEqualTo(ProfileErrorCode.USER_SKILL_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("2. Password & Secret Leakage Prevention")
    class PasswordLeakageTests {

        @Test
        @DisplayName("LEAK-01: UserProfileResponse JSON serialization must never contain passwordHash or password")
        void userProfileResponseMustNotContainPasswordHash() throws Exception {
            UserProfileResponse response = new UserProfileResponse(
                    userAId,
                    "victim@occupify.work",
                    "Victim User",
                    "Software Engineer",
                    "https://avatar.url",
                    "Bio text",
                    "+1234567890",
                    "USER",
                    "ACTIVE",
                    Instant.now(),
                    Instant.now()
            );

            String json = objectMapper.writeValueAsString(response);

            assertThat(json).doesNotContain("password");
            assertThat(json).doesNotContain("passwordHash");
            assertThat(json).doesNotContain("password_hash");
            assertThat(json).doesNotContain("hash");
        }

        @Test
        @DisplayName("LEAK-02: UserDetailResponse JSON serialization must never contain passwordHash")
        void userDetailResponseMustNotContainPasswordHash() throws Exception {
            UserProfileResponse profile = new UserProfileResponse(
                    userAId,
                    "victim@occupify.work",
                    "Victim User",
                    "Software Engineer",
                    "https://avatar.url",
                    "Bio text",
                    "+1234567890",
                    "USER",
                    "ACTIVE",
                    Instant.now(),
                    Instant.now()
            );

            UserDetailResponse detail = new UserDetailResponse(
                    profile,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            );

            String json = objectMapper.writeValueAsString(detail);

            assertThat(json).doesNotContain("password");
            assertThat(json).doesNotContain("passwordHash");
            assertThat(json).doesNotContain("password_hash");
        }

        @Test
        @DisplayName("LEAK-03: User entity serialization must respect @JsonIgnore on passwordHash")
        void userEntityMustNotSerializePasswordHash() throws Exception {
            User user = new User(
                    userAId,
                    "victim@occupify.work",
                    "$2a$12$e8YkZ7kR7cT4r1B6h7p5d.SuperSecretBcryptHashString123456",
                    UserRole.USER.name(),
                    UserStatus.ACTIVE.name(),
                    Instant.now(),
                    Instant.now()
            );

            String json = objectMapper.writeValueAsString(user);

            assertThat(json).doesNotContain("passwordHash");
            assertThat(json).doesNotContain("password_hash");
            assertThat(json).doesNotContain("SuperSecretBcryptHashString123456");
        }
    }
}
