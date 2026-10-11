package com.occupify.identity.controller.profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import com.occupify.identity.enums.EmploymentType;
import com.occupify.identity.enums.LocationType;
import com.occupify.identity.enums.ProficiencyLevel;
import com.occupify.identity.security.JwtUtils;
import com.occupify.identity.service.profile.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("UserProfileController Slice Tests (TDD RED State)")
class UserProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserProfileService userProfileService;

    @MockBean
    private JwtUtils jwtUtils;

    private ObjectMapper objectMapper;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        testUserId = UUID.randomUUID();
    }

    // =========================================================================
    // EXPERIENCES (POST, PUT, DELETE)
    // =========================================================================

    @Test
    @DisplayName("POST /api/v1/users/me/experiences -> should return 201 Created with CreatedResponse<ExperienceItemResponse>")
    void addExperience_success_shouldReturn201Created() throws Exception {
        UUID expId = UUID.randomUUID();
        CreateExperienceRequest request = new CreateExperienceRequest(
                "Senior Backend Engineer",
                "TechCorp Global",
                EmploymentType.FULL_TIME,
                "San Francisco, CA",
                LocationType.HYBRID,
                LocalDate.of(2022, 3, 1),
                null,
                true,
                "Architecting distributed microservices and message queues."
        );

        ExperienceItemResponse responseItem = new ExperienceItemResponse(
                expId,
                "Senior Backend Engineer",
                "TechCorp Global",
                EmploymentType.FULL_TIME.name(),
                "San Francisco, CA",
                LocationType.HYBRID.name(),
                LocalDate.of(2022, 3, 1),
                null,
                true,
                "Architecting distributed microservices and message queues."
        );

        when(userProfileService.addExperience(eq(testUserId), any(CreateExperienceRequest.class)))
                .thenReturn(responseItem);

        mockMvc.perform(post("/api/v1/users/me/experiences")
                        .header("X-User-Id", testUserId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.data.id").value(expId.toString()))
                .andExpect(jsonPath("$.data.title").value("Senior Backend Engineer"))
                .andExpect(jsonPath("$.data.organization").value("TechCorp Global"));
    }

    @Test
    @DisplayName("POST /api/v1/users/me/experiences -> should return 400 Bad Request when title or organization is blank")
    void addExperience_validationFailure_blankTitle_shouldReturn400() throws Exception {
        CreateExperienceRequest invalidRequest = new CreateExperienceRequest(
                "", // Blank title violates @NotBlank
                "TechCorp",
                EmploymentType.FULL_TIME,
                "Remote",
                LocationType.REMOTE,
                LocalDate.of(2023, 1, 1),
                null,
                true,
                "Summary"
        );

        mockMvc.perform(post("/api/v1/users/me/experiences")
                        .header("X-User-Id", testUserId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400));
    }

    @Test
    @DisplayName("PUT /api/v1/users/me/experiences/{id} -> should return 200 OK with SingleResponse<ExperienceItemResponse>")
    void updateExperience_success_shouldReturn200() throws Exception {
        UUID expId = UUID.randomUUID();
        UpdateExperienceRequest request = new UpdateExperienceRequest(
                "Lead Architect",
                "TechCorp Global",
                EmploymentType.FULL_TIME,
                "New York, NY",
                LocationType.ON_SITE,
                LocalDate.of(2022, 3, 1),
                LocalDate.of(2024, 6, 30),
                false,
                "Promoted to Lead Architect."
        );

        ExperienceItemResponse responseItem = new ExperienceItemResponse(
                expId,
                "Lead Architect",
                "TechCorp Global",
                EmploymentType.FULL_TIME.name(),
                "New York, NY",
                LocationType.ON_SITE.name(),
                LocalDate.of(2022, 3, 1),
                LocalDate.of(2024, 6, 30),
                false,
                "Promoted to Lead Architect."
        );

        when(userProfileService.updateExperience(eq(testUserId), eq(expId), any(UpdateExperienceRequest.class)))
                .thenReturn(responseItem);

        mockMvc.perform(put("/api/v1/users/me/experiences/{id}", expId)
                        .header("X-User-Id", testUserId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.id").value(expId.toString()))
                .andExpect(jsonPath("$.data.title").value("Lead Architect"));
    }

    @Test
    @DisplayName("DELETE /api/v1/users/me/experiences/{id} -> should return 200 OK with SuccessResponse")
    void deleteExperience_success_shouldReturn200() throws Exception {
        UUID expId = UUID.randomUUID();
        doNothing().when(userProfileService).deleteExperience(testUserId, expId);

        mockMvc.perform(delete("/api/v1/users/me/experiences/{id}", expId)
                        .header("X-User-Id", testUserId.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    // =========================================================================
    // EDUCATIONS (POST, PUT, DELETE)
    // =========================================================================

    @Test
    @DisplayName("POST /api/v1/users/me/educations -> should return 201 Created with CreatedResponse<EducationItemResponse>")
    void addEducation_success_shouldReturn201Created() throws Exception {
        UUID eduId = UUID.randomUUID();
        CreateEducationRequest request = new CreateEducationRequest(
                "Stanford University",
                "Computer Science",
                "Master of Science",
                "3.9 GPA",
                "Specialized in AI and Cloud Infrastructure",
                LocalDate.of(2018, 9, 1),
                LocalDate.of(2020, 6, 15)
        );

        EducationItemResponse responseItem = new EducationItemResponse(
                eduId,
                "Stanford University",
                "Computer Science",
                "Master of Science",
                "3.9 GPA",
                "Specialized in AI and Cloud Infrastructure",
                LocalDate.of(2018, 9, 1),
                LocalDate.of(2020, 6, 15)
        );

        when(userProfileService.addEducation(eq(testUserId), any(CreateEducationRequest.class)))
                .thenReturn(responseItem);

        mockMvc.perform(post("/api/v1/users/me/educations")
                        .header("X-User-Id", testUserId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.data.id").value(eduId.toString()))
                .andExpect(jsonPath("$.data.institution").value("Stanford University"))
                .andExpect(jsonPath("$.data.degree").value("Master of Science"));
    }

    @Test
    @DisplayName("POST /api/v1/users/me/educations -> should return 400 Bad Request when institution is blank")
    void addEducation_validationFailure_blankInstitution_shouldReturn400() throws Exception {
        CreateEducationRequest invalidRequest = new CreateEducationRequest(
                "", // Blank institution violates @NotBlank
                "Data Science",
                "Bachelor",
                "3.8",
                "Description",
                LocalDate.of(2015, 9, 1),
                LocalDate.of(2019, 6, 1)
        );

        mockMvc.perform(post("/api/v1/users/me/educations")
                        .header("X-User-Id", testUserId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400));
    }

    @Test
    @DisplayName("PUT /api/v1/users/me/educations/{id} -> should return 200 OK with SingleResponse<EducationItemResponse>")
    void updateEducation_success_shouldReturn200() throws Exception {
        UUID eduId = UUID.randomUUID();
        UpdateEducationRequest request = new UpdateEducationRequest(
                "Stanford University",
                "Software Engineering",
                "Master of Science (Honors)",
                "4.0 GPA",
                "Graduated with Distinction",
                LocalDate.of(2018, 9, 1),
                LocalDate.of(2020, 6, 15)
        );

        EducationItemResponse responseItem = new EducationItemResponse(
                eduId,
                "Stanford University",
                "Software Engineering",
                "Master of Science (Honors)",
                "4.0 GPA",
                "Graduated with Distinction",
                LocalDate.of(2018, 9, 1),
                LocalDate.of(2020, 6, 15)
        );

        when(userProfileService.updateEducation(eq(testUserId), eq(eduId), any(UpdateEducationRequest.class)))
                .thenReturn(responseItem);

        mockMvc.perform(put("/api/v1/users/me/educations/{id}", eduId)
                        .header("X-User-Id", testUserId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.id").value(eduId.toString()))
                .andExpect(jsonPath("$.data.degree").value("Master of Science (Honors)"));
    }

    @Test
    @DisplayName("DELETE /api/v1/users/me/educations/{id} -> should return 200 OK with SuccessResponse")
    void deleteEducation_success_shouldReturn200() throws Exception {
        UUID eduId = UUID.randomUUID();
        doNothing().when(userProfileService).deleteEducation(testUserId, eduId);

        mockMvc.perform(delete("/api/v1/users/me/educations/{id}", eduId)
                        .header("X-User-Id", testUserId.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    // =========================================================================
    // QUALIFICATIONS (POST, PUT, DELETE)
    // =========================================================================

    @Test
    @DisplayName("POST /api/v1/users/me/qualifications -> should return 201 Created with CreatedResponse<QualificationItemResponse>")
    void addQualification_success_shouldReturn201Created() throws Exception {
        UUID qualId = UUID.randomUUID();
        CreateQualificationRequest request = new CreateQualificationRequest(
                "AWS Certified Solutions Architect - Professional",
                "Amazon Web Services",
                LocalDate.of(2023, 5, 10),
                LocalDate.of(2026, 5, 10),
                "AWS-PRO-98765",
                "https://aws.amazon.com/verification/AWS-PRO-98765"
        );

        QualificationItemResponse responseItem = new QualificationItemResponse(
                qualId,
                "AWS Certified Solutions Architect - Professional",
                "Amazon Web Services",
                LocalDate.of(2023, 5, 10),
                LocalDate.of(2026, 5, 10),
                "AWS-PRO-98765",
                "https://aws.amazon.com/verification/AWS-PRO-98765"
        );

        when(userProfileService.addQualification(eq(testUserId), any(CreateQualificationRequest.class)))
                .thenReturn(responseItem);

        mockMvc.perform(post("/api/v1/users/me/qualifications")
                        .header("X-User-Id", testUserId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.data.id").value(qualId.toString()))
                .andExpect(jsonPath("$.data.title").value("AWS Certified Solutions Architect - Professional"))
                .andExpect(jsonPath("$.data.organization").value("Amazon Web Services"));
    }

    @Test
    @DisplayName("POST /api/v1/users/me/qualifications -> should return 400 Bad Request when title is blank")
    void addQualification_validationFailure_blankTitle_shouldReturn400() throws Exception {
        CreateQualificationRequest invalidRequest = new CreateQualificationRequest(
                "", // Blank title violates @NotBlank
                "AWS",
                LocalDate.of(2023, 1, 1),
                null,
                "ID123",
                "https://verify.url"
        );

        mockMvc.perform(post("/api/v1/users/me/qualifications")
                        .header("X-User-Id", testUserId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400));
    }

    @Test
    @DisplayName("PUT /api/v1/users/me/qualifications/{id} -> should return 200 OK with SingleResponse<QualificationItemResponse>")
    void updateQualification_success_shouldReturn200() throws Exception {
        UUID qualId = UUID.randomUUID();
        UpdateQualificationRequest request = new UpdateQualificationRequest(
                "AWS Certified Solutions Architect - Professional (Renewed)",
                "Amazon Web Services",
                LocalDate.of(2023, 5, 10),
                LocalDate.of(2029, 5, 10),
                "AWS-PRO-98765-V2",
                "https://aws.amazon.com/verification/AWS-PRO-98765-V2"
        );

        QualificationItemResponse responseItem = new QualificationItemResponse(
                qualId,
                "AWS Certified Solutions Architect - Professional (Renewed)",
                "Amazon Web Services",
                LocalDate.of(2023, 5, 10),
                LocalDate.of(2029, 5, 10),
                "AWS-PRO-98765-V2",
                "https://aws.amazon.com/verification/AWS-PRO-98765-V2"
        );

        when(userProfileService.updateQualification(eq(testUserId), eq(qualId), any(UpdateQualificationRequest.class)))
                .thenReturn(responseItem);

        mockMvc.perform(put("/api/v1/users/me/qualifications/{id}", qualId)
                        .header("X-User-Id", testUserId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.id").value(qualId.toString()))
                .andExpect(jsonPath("$.data.credentialId").value("AWS-PRO-98765-V2"));
    }

    @Test
    @DisplayName("DELETE /api/v1/users/me/qualifications/{id} -> should return 200 OK with SuccessResponse")
    void deleteQualification_success_shouldReturn200() throws Exception {
        UUID qualId = UUID.randomUUID();
        doNothing().when(userProfileService).deleteQualification(testUserId, qualId);

        mockMvc.perform(delete("/api/v1/users/me/qualifications/{id}", qualId)
                        .header("X-User-Id", testUserId.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    // =========================================================================
    // SKILLS (POST, PUT, DELETE, CATALOG GET)
    // =========================================================================

    @Test
    @DisplayName("POST /api/v1/users/me/skills -> should return 201 Created with CreatedResponse<UserSkillItemResponse>")
    void addSkill_success_shouldReturn201Created() throws Exception {
        UUID skillId = UUID.randomUUID();
        AddUserSkillRequest request = new AddUserSkillRequest(
                skillId,
                "Java",
                "Programming Languages",
                true,
                ProficiencyLevel.EXPERT,
                6
        );

        UserSkillItemResponse responseItem = new UserSkillItemResponse(
                skillId,
                "Java",
                "Programming Languages",
                true,
                ProficiencyLevel.EXPERT.name(),
                6
        );

        when(userProfileService.addSkill(eq(testUserId), any(AddUserSkillRequest.class)))
                .thenReturn(responseItem);

        mockMvc.perform(post("/api/v1/users/me/skills")
                        .header("X-User-Id", testUserId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.data.skillId").value(skillId.toString()))
                .andExpect(jsonPath("$.data.skillName").value("Java"))
                .andExpect(jsonPath("$.data.isTopSkill").value(true));
    }

    @Test
    @DisplayName("PUT /api/v1/users/me/skills/{skillId} -> should return 200 OK with SingleResponse<UserSkillItemResponse>")
    void updateSkill_success_shouldReturn200() throws Exception {
        UUID skillId = UUID.randomUUID();
        UpdateUserSkillRequest request = new UpdateUserSkillRequest(
                false,
                ProficiencyLevel.ADVANCED,
                7
        );

        UserSkillItemResponse responseItem = new UserSkillItemResponse(
                skillId,
                "Java",
                "Programming Languages",
                false,
                ProficiencyLevel.ADVANCED.name(),
                7
        );

        when(userProfileService.updateSkill(eq(testUserId), eq(skillId), any(UpdateUserSkillRequest.class)))
                .thenReturn(responseItem);

        mockMvc.perform(put("/api/v1/users/me/skills/{skillId}", skillId)
                        .header("X-User-Id", testUserId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.skillId").value(skillId.toString()))
                .andExpect(jsonPath("$.data.proficiencyLevel").value("ADVANCED"))
                .andExpect(jsonPath("$.data.yearsOfExperience").value(7));
    }

    @Test
    @DisplayName("DELETE /api/v1/users/me/skills/{skillId} -> should return 200 OK with SuccessResponse")
    void removeSkill_success_shouldReturn200() throws Exception {
        UUID skillId = UUID.randomUUID();
        doNothing().when(userProfileService).removeSkill(testUserId, skillId);

        mockMvc.perform(delete("/api/v1/users/me/skills/{skillId}", skillId)
                        .header("X-User-Id", testUserId.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("GET /api/v1/skills -> should return 200 OK with SingleResponse<List<SkillResponse>>")
    void searchSkills_success_shouldReturn200AndSkillList() throws Exception {
        UUID s1 = UUID.randomUUID();
        UUID s2 = UUID.randomUUID();
        List<SkillResponse> mockCatalog = List.of(
                new SkillResponse(s1, "Spring Boot", "Frameworks"),
                new SkillResponse(s2, "Spring Cloud", "Frameworks")
        );

        when(userProfileService.searchSkills("spring")).thenReturn(mockCatalog);

        mockMvc.perform(get("/api/v1/skills")
                        .param("keyword", "spring")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].name").value("Spring Boot"))
                .andExpect(jsonPath("$.data[1].name").value("Spring Cloud"));
    }
}
