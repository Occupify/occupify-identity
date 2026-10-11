package com.occupify.identity.controller.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.occupify.identity.dto.request.user.UpdateUserProfileRequest;
import com.occupify.identity.dto.response.user.UserDetailResponse;
import com.occupify.identity.dto.response.user.UserProfileResponse;
import com.occupify.identity.security.JwtUtils;
import com.occupify.identity.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("UserController Slice Tests (TDD RED State)")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtUtils jwtUtils;

    private ObjectMapper objectMapper;
    private UUID testUserId;
    private UserProfileResponse mockProfileResponse;
    private UserDetailResponse mockDetailResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        testUserId = UUID.randomUUID();
        mockProfileResponse = new UserProfileResponse(
                testUserId,
                "freelancer@occupify.work",
                "Alex Hunter",
                "Senior Full Stack Engineer",
                "https://cdn.occupify.work/avatars/alex.png",
                "Passionate about distributed systems and cloud architecture.",
                "+84901234567",
                "USER",
                "ACTIVE",
                Instant.now(),
                Instant.now()
        );

        mockDetailResponse = new UserDetailResponse(
                mockProfileResponse,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        );
    }

    @Test
    @DisplayName("GET /api/v1/users/me -> should return 200 OK with SingleResponse<UserProfileResponse>")
    void getMyProfile_success_shouldReturn200AndUserProfile() throws Exception {
        when(userService.getMyProfile(testUserId)).thenReturn(mockProfileResponse);

        mockMvc.perform(get("/api/v1/users/me")
                        .header("X-User-Id", testUserId.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.id").value(testUserId.toString()))
                .andExpect(jsonPath("$.data.email").value("freelancer@occupify.work"))
                .andExpect(jsonPath("$.data.fullName").value("Alex Hunter"))
                .andExpect(jsonPath("$.data.role").value("USER"));
    }

    @Test
    @DisplayName("GET /api/v1/users/me/detail -> should return 200 OK with SingleResponse<UserDetailResponse>")
    void getMyProfileDetail_success_shouldReturn200AndFullDetail() throws Exception {
        when(userService.getMyProfileDetail(testUserId)).thenReturn(mockDetailResponse);

        mockMvc.perform(get("/api/v1/users/me/detail")
                        .header("X-User-Id", testUserId.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.user.id").value(testUserId.toString()))
                .andExpect(jsonPath("$.data.user.fullName").value("Alex Hunter"))
                .andExpect(jsonPath("$.data.experiences").isArray())
                .andExpect(jsonPath("$.data.educations").isArray())
                .andExpect(jsonPath("$.data.qualifications").isArray())
                .andExpect(jsonPath("$.data.skills").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/users/{id}/detail -> should return 200 OK for public profile view")
    void getPublicUserDetail_success_shouldReturn200AndPublicProfile() throws Exception {
        UUID publicUserId = UUID.randomUUID();
        UserProfileResponse publicProfile = new UserProfileResponse(
                publicUserId,
                "public@occupify.work",
                "Sophia Chen",
                "Lead UI/UX Designer",
                "https://cdn.occupify.work/avatars/sophia.png",
                "Designing delightful digital products.",
                "+84909876543",
                "USER",
                "ACTIVE",
                Instant.now(),
                Instant.now()
        );
        UserDetailResponse publicDetail = new UserDetailResponse(
                publicProfile,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        );

        when(userService.getPublicUserDetail(publicUserId)).thenReturn(publicDetail);

        mockMvc.perform(get("/api/v1/users/{id}/detail", publicUserId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.user.id").value(publicUserId.toString()))
                .andExpect(jsonPath("$.data.user.fullName").value("Sophia Chen"));
    }

    @Test
    @DisplayName("PUT /api/v1/users/me -> should return 200 OK with SingleResponse<UserProfileResponse> when valid")
    void updateMyProfile_success_shouldReturn200AndUpdatedProfile() throws Exception {
        UpdateUserProfileRequest request = new UpdateUserProfileRequest(
                "Alex Updated",
                "Staff Software Engineer",
                "Updated bio narrative.",
                "+84901234567"
        );

        UserProfileResponse updatedProfile = new UserProfileResponse(
                testUserId,
                "freelancer@occupify.work",
                "Alex Updated",
                "Staff Software Engineer",
                "https://cdn.occupify.work/avatars/alex-updated.png",
                "Updated bio narrative.",
                "+84901234567",
                "USER",
                "ACTIVE",
                Instant.now(),
                Instant.now()
        );

        when(userService.updateMyProfile(eq(testUserId), any(UpdateUserProfileRequest.class))).thenReturn(updatedProfile);

        mockMvc.perform(put("/api/v1/users/me")
                        .header("X-User-Id", testUserId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.fullName").value("Alex Updated"))
                .andExpect(jsonPath("$.data.headline").value("Staff Software Engineer"));
    }

    @Test
    @DisplayName("PUT /api/v1/users/me -> should return 400 Bad Request when fullName is blank")
    void updateMyProfile_validationFailure_blankFullName_shouldReturn400() throws Exception {
        UpdateUserProfileRequest invalidRequest = new UpdateUserProfileRequest(
                "", // Blank full name violates @NotBlank
                "Staff Engineer",
                "Bio",
                "+84901234567"
        );

        mockMvc.perform(put("/api/v1/users/me")
                        .header("X-User-Id", testUserId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400));
    }

    @Test
    @DisplayName("DELETE /api/v1/users/me -> should return 200 OK with SuccessResponse")
    void deleteMyAccount_success_shouldReturn200SuccessEnvelope() throws Exception {
        doNothing().when(userService).deleteMyAccount(testUserId);

        mockMvc.perform(delete("/api/v1/users/me")
                        .header("X-User-Id", testUserId.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }
}
