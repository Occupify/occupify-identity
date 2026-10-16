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
import com.occupify.identity.dto.response.profile.InstitutionResponse;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
    @DisplayName("POST /users/me/experiences -> should return 201 Created with CreatedResponse<ExperienceItemResponse>")
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

        mockMvc.perform(post("/users/me/experiences")
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
    @DisplayName("POST /users/me/experiences -> should return 400 Bad Request when title or organization is blank")
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

        mockMvc.perform(post("/users/me/experiences")
                        .header("X-User-Id", testUserId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400));
    }

    @Test
    @DisplayName("PUT /users/me/experiences/{id} -> should return 200 OK with SingleResponse<ExperienceItemResponse>")
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

        mockMvc.perform(put("/users/me/experiences/{id}", expId)
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
    @DisplayName("DELETE /users/me/experiences/{id} -> should return 200 OK with SuccessResponse")
    void deleteExperience_success_shouldReturn200() throws Exception {
        UUID expId = UUID.randomUUID();
        doNothing().when(userProfileService).deleteExperience(testUserId, expId);

        mockMvc.perform(delete("/users/me/experiences/{id}", expId)
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
    @DisplayName("POST /users/me/educations -> should return 201 Created with CreatedResponse<EducationItemResponse>")
    void addEducation_success_shouldReturn201Created() throws Exception {
        UUID eduId = UUID.randomUUID();
        UUID instId = UUID.randomUUID();
        MockMultipartFile docFile = new MockMultipartFile("document", "diploma.pdf", "application/pdf", "dummy pdf".getBytes());

        EducationItemResponse responseItem = new EducationItemResponse(
                eduId,
                instId,
                "Stanford University",
                "https://stanford.edu/logo.png",
                "Computer Science",
                "Master of Science",
                "3.9 GPA",
                "Specialized in AI and Cloud Infrastructure",
                LocalDate.of(2018, 9, 1),
                LocalDate.of(2020, 6, 15),
                "https://s3.amazonaws.com/bucket/doc.pdf"
        );

        when(userProfileService.addEducation(eq(testUserId), any(CreateEducationRequest.class)))
                .thenReturn(responseItem);

        mockMvc.perform(multipart("/users/me/educations")
                        .file(docFile)
                        .param("institutionId", instId.toString())
                        .param("fieldOfStudy", "Computer Science")
                        .param("degree", "Master of Science")
                        .param("grade", "3.9 GPA")
                        .param("description", "Specialized in AI and Cloud Infrastructure")
                        .param("startDate", "2018-09-01")
                        .param("endDate", "2020-06-15")
                        .header("X-User-Id", testUserId.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.data.id").value(eduId.toString()))
                .andExpect(jsonPath("$.data.institutionName").value("Stanford University"))
                .andExpect(jsonPath("$.data.documentUrl").value("https://s3.amazonaws.com/bucket/doc.pdf"));
    }

    @Test
    @DisplayName("POST /users/me/educations -> should return 400 Bad Request when institutionId is missing")
    void addEducation_validationFailure_blankInstitution_shouldReturn400() throws Exception {
        mockMvc.perform(multipart("/users/me/educations")
                        .param("fieldOfStudy", "Data Science")
                        .param("startDate", "2015-09-01")
                        .header("X-User-Id", testUserId.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400));
    }

    @Test
    @DisplayName("PUT /users/me/educations/{id} -> should return 200 OK with SingleResponse<EducationItemResponse>")
    void updateEducation_success_shouldReturn200() throws Exception {
        UUID eduId = UUID.randomUUID();
        UUID instId = UUID.randomUUID();

        EducationItemResponse responseItem = new EducationItemResponse(
                eduId,
                instId,
                "Stanford University",
                "https://stanford.edu/logo.png",
                "Software Engineering",
                "Master of Science (Honors)",
                "4.0 GPA",
                "Graduated with Distinction",
                LocalDate.of(2018, 9, 1),
                LocalDate.of(2020, 6, 15),
                "https://s3.amazonaws.com/bucket/new-doc.pdf"
        );

        when(userProfileService.updateEducation(eq(testUserId), eq(eduId), any(UpdateEducationRequest.class)))
                .thenReturn(responseItem);

        mockMvc.perform(multipart(HttpMethod.PUT, "/users/me/educations/{id}", eduId)
                        .param("institutionId", instId.toString())
                        .param("fieldOfStudy", "Software Engineering")
                        .param("degree", "Master of Science (Honors)")
                        .param("grade", "4.0 GPA")
                        .param("description", "Graduated with Distinction")
                        .param("startDate", "2018-09-01")
                        .param("endDate", "2020-06-15")
                        .header("X-User-Id", testUserId.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.id").value(eduId.toString()))
                .andExpect(jsonPath("$.data.degree").value("Master of Science (Honors)"));
    }

    @Test
    @DisplayName("DELETE /users/me/educations/{id} -> should return 200 OK with SuccessResponse")
    void deleteEducation_success_shouldReturn200() throws Exception {
        UUID eduId = UUID.randomUUID();
        doNothing().when(userProfileService).deleteEducation(testUserId, eduId);

        mockMvc.perform(delete("/users/me/educations/{id}", eduId)
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
    @DisplayName("POST /users/me/qualifications -> should return 201 Created with CreatedResponse<QualificationItemResponse>")
    void addQualification_success_shouldReturn201Created() throws Exception {
        UUID qualId = UUID.randomUUID();
        MockMultipartFile docFile = new MockMultipartFile("document", "cert.pdf", "application/pdf", "dummy cert".getBytes());

        QualificationItemResponse responseItem = new QualificationItemResponse(
                qualId,
                "AWS Certified Solutions Architect - Professional",
                "Amazon Web Services",
                LocalDate.of(2023, 5, 10),
                LocalDate.of(2026, 5, 10),
                "AWS-PRO-98765",
                "https://aws.amazon.com/verification/AWS-PRO-98765",
                "https://s3.amazonaws.com/bucket/cert.pdf"
        );

        when(userProfileService.addQualification(eq(testUserId), any(CreateQualificationRequest.class)))
                .thenReturn(responseItem);

        mockMvc.perform(multipart("/users/me/qualifications")
                        .file(docFile)
                        .param("title", "AWS Certified Solutions Architect - Professional")
                        .param("organization", "Amazon Web Services")
                        .param("issueDate", "2023-05-10")
                        .param("expirationDate", "2026-05-10")
                        .param("credentialId", "AWS-PRO-98765")
                        .param("credentialUrl", "https://aws.amazon.com/verification/AWS-PRO-98765")
                        .header("X-User-Id", testUserId.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.data.id").value(qualId.toString()))
                .andExpect(jsonPath("$.data.title").value("AWS Certified Solutions Architect - Professional"))
                .andExpect(jsonPath("$.data.documentUrl").value("https://s3.amazonaws.com/bucket/cert.pdf"));
    }

    @Test
    @DisplayName("POST /users/me/qualifications -> should return 400 Bad Request when title is blank")
    void addQualification_validationFailure_blankTitle_shouldReturn400() throws Exception {
        mockMvc.perform(multipart("/users/me/qualifications")
                        .param("title", "")
                        .param("organization", "AWS")
                        .param("issueDate", "2023-01-01")
                        .header("X-User-Id", testUserId.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400));
    }

    @Test
    @DisplayName("PUT /users/me/qualifications/{id} -> should return 200 OK with SingleResponse<QualificationItemResponse>")
    void updateQualification_success_shouldReturn200() throws Exception {
        UUID qualId = UUID.randomUUID();

        QualificationItemResponse responseItem = new QualificationItemResponse(
                qualId,
                "AWS Certified Solutions Architect - Professional (Renewed)",
                "Amazon Web Services",
                LocalDate.of(2023, 5, 10),
                LocalDate.of(2029, 5, 10),
                "AWS-PRO-98765-V2",
                "https://aws.amazon.com/verification/AWS-PRO-98765-V2",
                "https://s3.amazonaws.com/bucket/new-cert.pdf"
        );

        when(userProfileService.updateQualification(eq(testUserId), eq(qualId), any(UpdateQualificationRequest.class)))
                .thenReturn(responseItem);

        mockMvc.perform(multipart(HttpMethod.PUT, "/users/me/qualifications/{id}", qualId)
                        .param("title", "AWS Certified Solutions Architect - Professional (Renewed)")
                        .param("organization", "Amazon Web Services")
                        .param("issueDate", "2023-05-10")
                        .param("expirationDate", "2029-05-10")
                        .param("credentialId", "AWS-PRO-98765-V2")
                        .param("credentialUrl", "https://aws.amazon.com/verification/AWS-PRO-98765-V2")
                        .header("X-User-Id", testUserId.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.id").value(qualId.toString()))
                .andExpect(jsonPath("$.data.credentialId").value("AWS-PRO-98765-V2"));
    }

    @Test
    @DisplayName("DELETE /users/me/qualifications/{id} -> should return 200 OK with SuccessResponse")
    void deleteQualification_success_shouldReturn200() throws Exception {
        UUID qualId = UUID.randomUUID();
        doNothing().when(userProfileService).deleteQualification(testUserId, qualId);

        mockMvc.perform(delete("/users/me/qualifications/{id}", qualId)
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
    @DisplayName("POST /users/me/skills -> should return 201 Created with CreatedResponse<UserSkillItemResponse>")
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

        mockMvc.perform(post("/users/me/skills")
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
    @DisplayName("PUT /users/me/skills/{skillId} -> should return 200 OK with SingleResponse<UserSkillItemResponse>")
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

        mockMvc.perform(put("/users/me/skills/{skillId}", skillId)
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
    @DisplayName("DELETE /users/me/skills/{skillId} -> should return 200 OK with SuccessResponse")
    void removeSkill_success_shouldReturn200() throws Exception {
        UUID skillId = UUID.randomUUID();
        doNothing().when(userProfileService).removeSkill(testUserId, skillId);

        mockMvc.perform(delete("/users/me/skills/{skillId}", skillId)
                        .header("X-User-Id", testUserId.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("GET /users/me/skills -> should return 200 OK with SingleResponse<List<SkillResponse>>")
    void searchSkills_success_shouldReturn200AndSkillList() throws Exception {
        UUID s1 = UUID.randomUUID();
        UUID s2 = UUID.randomUUID();
        List<SkillResponse> mockCatalog = List.of(
                new SkillResponse(s1, "Spring Boot", "Frameworks"),
                new SkillResponse(s2, "Spring Cloud", "Frameworks")
        );

        when(userProfileService.searchSkills("spring")).thenReturn(mockCatalog);

        mockMvc.perform(get("/users/me/skills")
                        .param("keyword", "spring")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].name").value("Spring Boot"))
                .andExpect(jsonPath("$.data[1].name").value("Spring Cloud"));
    }

    @Test
    @DisplayName("GET /users/me/institutions -> should return 200 OK with SingleResponse<List<InstitutionResponse>>")
    void searchInstitutions_success_shouldReturn200AndInstitutionList() throws Exception {
        UUID instId1 = UUID.randomUUID();
        UUID instId2 = UUID.randomUUID();
        List<InstitutionResponse> mockList = List.of(
                new InstitutionResponse(instId1, "Hanoi University of Science and Technology", "HUST", "Vietnam", "Hanoi", "https://hust.edu.vn", "https://logo.com/hust.png"),
                new InstitutionResponse(instId2, "Vietnam National University - UET", "UET", "Vietnam", "Hanoi", "https://uet.vnu.edu.vn", "https://logo.com/uet.png")
        );

        when(userProfileService.searchInstitutions("tech")).thenReturn(mockList);

        mockMvc.perform(get("/users/me/institutions")
                        .param("keyword", "tech")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].name").value("Hanoi University of Science and Technology"))
                .andExpect(jsonPath("$.data[0].code").value("HUST"))
                .andExpect(jsonPath("$.data[1].name").value("Vietnam National University - UET"));
    }
}
