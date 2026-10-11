package com.occupify.identity.controller.profile;

import com.occupify.identity.config.OpenApiConfig;
import com.occupify.identity.controller.AbstractBaseController;
import com.occupify.identity.dto.base.CreatedResponse;
import com.occupify.identity.dto.base.SingleResponse;
import com.occupify.identity.dto.base.SuccessResponse;
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
import com.occupify.identity.security.JwtUtils;
import com.occupify.identity.service.profile.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping
@Tag(name = "User Profile Sub-Resources", description = "Endpoints for managing candidate career history (experiences, educations, qualifications, and skills)")
public class UserProfileController extends AbstractBaseController {

    private final UserProfileService userProfileService;
    private final JwtUtils jwtUtils;

    public UserProfileController(UserProfileService userProfileService, JwtUtils jwtUtils) {
        this.userProfileService = userProfileService;
        this.jwtUtils = jwtUtils;
    }

    // =========================================================================
    // EXPERIENCES
    // =========================================================================

    @PostMapping("/api/v1/users/me/experiences")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Add work experience",
            description = "Appends a new work experience entry to the authenticated user's profile",
            security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
    )
    public CreatedResponse<ExperienceItemResponse> addExperience(
            @Parameter(description = "Injected user ID from Gateway", hidden = true)
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @Valid @RequestBody CreateExperienceRequest request,
            HttpServletRequest servletRequest
    ) {
        UUID currentUserId = resolveUserId(headerUserId, servletRequest, jwtUtils);
        log.info("[POST /api/v1/users/me/experiences] Adding experience for user: {}", currentUserId);
        ExperienceItemResponse response = userProfileService.addExperience(currentUserId, request);
        return created(response, "Work experience created successfully");
    }

    @PutMapping("/api/v1/users/me/experiences/{id}")
    @Operation(
            summary = "Update work experience",
            description = "Updates an existing work experience item belonging to the current user",
            security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
    )
    public SingleResponse<ExperienceItemResponse> updateExperience(
            @Parameter(description = "Injected user ID from Gateway", hidden = true)
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @PathVariable("id") UUID experienceId,
            @Valid @RequestBody UpdateExperienceRequest request,
            HttpServletRequest servletRequest
    ) {
        UUID currentUserId = resolveUserId(headerUserId, servletRequest, jwtUtils);
        log.info("[PUT /api/v1/users/me/experiences/{}] Updating experience for user: {}", experienceId, currentUserId);
        ExperienceItemResponse response = userProfileService.updateExperience(currentUserId, experienceId, request);
        return successSingle(response, "Work experience updated successfully");
    }

    @DeleteMapping("/api/v1/users/me/experiences/{id}")
    @Operation(
            summary = "Delete work experience",
            description = "Removes a work experience record from the current user's profile",
            security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
    )
    public SuccessResponse deleteExperience(
            @Parameter(description = "Injected user ID from Gateway", hidden = true)
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @PathVariable("id") UUID experienceId,
            HttpServletRequest servletRequest
    ) {
        UUID currentUserId = resolveUserId(headerUserId, servletRequest, jwtUtils);
        log.warn("[DELETE /api/v1/users/me/experiences/{}] Deleting experience for user: {}", experienceId, currentUserId);
        userProfileService.deleteExperience(currentUserId, experienceId);
        return success("Work experience deleted successfully");
    }

    // =========================================================================
    // EDUCATIONS
    // =========================================================================

    @PostMapping("/api/v1/users/me/educations")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Add education record",
            description = "Appends a new academic education record to current user's profile",
            security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
    )
    public CreatedResponse<EducationItemResponse> addEducation(
            @Parameter(description = "Injected user ID from Gateway", hidden = true)
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @Valid @RequestBody CreateEducationRequest request,
            HttpServletRequest servletRequest
    ) {
        UUID currentUserId = resolveUserId(headerUserId, servletRequest, jwtUtils);
        log.info("[POST /api/v1/users/me/educations] Adding education for user: {}", currentUserId);
        EducationItemResponse response = userProfileService.addEducation(currentUserId, request);
        return created(response, "Education record created successfully");
    }

    @PutMapping("/api/v1/users/me/educations/{id}")
    @Operation(
            summary = "Update education record",
            description = "Updates an existing education record belonging to the current user",
            security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
    )
    public SingleResponse<EducationItemResponse> updateEducation(
            @Parameter(description = "Injected user ID from Gateway", hidden = true)
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @PathVariable("id") UUID educationId,
            @Valid @RequestBody UpdateEducationRequest request,
            HttpServletRequest servletRequest
    ) {
        UUID currentUserId = resolveUserId(headerUserId, servletRequest, jwtUtils);
        log.info("[PUT /api/v1/users/me/educations/{}] Updating education for user: {}", educationId, currentUserId);
        EducationItemResponse response = userProfileService.updateEducation(currentUserId, educationId, request);
        return successSingle(response, "Education record updated successfully");
    }

    @DeleteMapping("/api/v1/users/me/educations/{id}")
    @Operation(
            summary = "Delete education record",
            description = "Removes an education record from current user's profile",
            security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
    )
    public SuccessResponse deleteEducation(
            @Parameter(description = "Injected user ID from Gateway", hidden = true)
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @PathVariable("id") UUID educationId,
            HttpServletRequest servletRequest
    ) {
        UUID currentUserId = resolveUserId(headerUserId, servletRequest, jwtUtils);
        log.warn("[DELETE /api/v1/users/me/educations/{}] Deleting education for user: {}", educationId, currentUserId);
        userProfileService.deleteEducation(currentUserId, educationId);
        return success("Education record deleted successfully");
    }

    // =========================================================================
    // QUALIFICATIONS
    // =========================================================================

    @PostMapping("/api/v1/users/me/qualifications")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Add qualification or certification",
            description = "Appends a professional certification to current user's profile",
            security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
    )
    public CreatedResponse<QualificationItemResponse> addQualification(
            @Parameter(description = "Injected user ID from Gateway", hidden = true)
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @Valid @RequestBody CreateQualificationRequest request,
            HttpServletRequest servletRequest
    ) {
        UUID currentUserId = resolveUserId(headerUserId, servletRequest, jwtUtils);
        log.info("[POST /api/v1/users/me/qualifications] Adding qualification for user: {}", currentUserId);
        QualificationItemResponse response = userProfileService.addQualification(currentUserId, request);
        return created(response, "Qualification record created successfully");
    }

    @PutMapping("/api/v1/users/me/qualifications/{id}")
    @Operation(
            summary = "Update qualification record",
            description = "Updates an existing qualification record belonging to current user",
            security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
    )
    public SingleResponse<QualificationItemResponse> updateQualification(
            @Parameter(description = "Injected user ID from Gateway", hidden = true)
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @PathVariable("id") UUID qualificationId,
            @Valid @RequestBody UpdateQualificationRequest request,
            HttpServletRequest servletRequest
    ) {
        UUID currentUserId = resolveUserId(headerUserId, servletRequest, jwtUtils);
        log.info("[PUT /api/v1/users/me/qualifications/{}] Updating qualification for user: {}", qualificationId, currentUserId);
        QualificationItemResponse response = userProfileService.updateQualification(currentUserId, qualificationId, request);
        return successSingle(response, "Qualification record updated successfully");
    }

    @DeleteMapping("/api/v1/users/me/qualifications/{id}")
    @Operation(
            summary = "Delete qualification record",
            description = "Removes a qualification record from current user's profile",
            security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
    )
    public SuccessResponse deleteQualification(
            @Parameter(description = "Injected user ID from Gateway", hidden = true)
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @PathVariable("id") UUID qualificationId,
            HttpServletRequest servletRequest
    ) {
        UUID currentUserId = resolveUserId(headerUserId, servletRequest, jwtUtils);
        log.warn("[DELETE /api/v1/users/me/qualifications/{}] Deleting qualification for user: {}", qualificationId, currentUserId);
        userProfileService.deleteQualification(currentUserId, qualificationId);
        return success("Qualification record deleted successfully");
    }

    // =========================================================================
    // SKILLS
    // =========================================================================

    @PostMapping("/api/v1/users/me/skills")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Attach skill to profile",
            description = "Attaches a new technical skill or competency to current user's profile",
            security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
    )
    public CreatedResponse<UserSkillItemResponse> addSkill(
            @Parameter(description = "Injected user ID from Gateway", hidden = true)
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @Valid @RequestBody AddUserSkillRequest request,
            HttpServletRequest servletRequest
    ) {
        UUID currentUserId = resolveUserId(headerUserId, servletRequest, jwtUtils);
        log.info("[POST /api/v1/users/me/skills] Attaching skill for user: {}", currentUserId);
        UserSkillItemResponse response = userProfileService.addSkill(currentUserId, request);
        return created(response, "Skill attached to profile successfully");
    }

    @PutMapping("/api/v1/users/me/skills/{skillId}")
    @Operation(
            summary = "Update skill attributes on profile",
            description = "Updates proficiency level, top-skill flag, or years of experience for a skill",
            security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
    )
    public SingleResponse<UserSkillItemResponse> updateSkill(
            @Parameter(description = "Injected user ID from Gateway", hidden = true)
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @PathVariable("skillId") UUID skillId,
            @Valid @RequestBody UpdateUserSkillRequest request,
            HttpServletRequest servletRequest
    ) {
        UUID currentUserId = resolveUserId(headerUserId, servletRequest, jwtUtils);
        log.info("[PUT /api/v1/users/me/skills/{}] Updating skill for user: {}", skillId, currentUserId);
        UserSkillItemResponse response = userProfileService.updateSkill(currentUserId, skillId, request);
        return successSingle(response, "Skill updated successfully");
    }

    @DeleteMapping("/api/v1/users/me/skills/{skillId}")
    @Operation(
            summary = "Remove skill from profile",
            description = "Detaches a skill from current user's profile",
            security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
    )
    public SuccessResponse removeSkill(
            @Parameter(description = "Injected user ID from Gateway", hidden = true)
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @PathVariable("skillId") UUID skillId,
            HttpServletRequest servletRequest
    ) {
        UUID currentUserId = resolveUserId(headerUserId, servletRequest, jwtUtils);
        log.warn("[DELETE /api/v1/users/me/skills/{}] Removing skill for user: {}", skillId, currentUserId);
        userProfileService.removeSkill(currentUserId, skillId);
        return success("Skill removed from profile successfully");
    }

    @GetMapping("/api/v1/skills")
    @Operation(
            summary = "Search system skills catalog",
            description = "Searches skills catalog by name substring or retrieves entire catalog when keyword is empty"
    )
    public SingleResponse<List<SkillResponse>> searchSkills(
            @Parameter(description = "Keyword to filter skill catalog by name")
            @RequestParam(value = "keyword", required = false) String keyword
    ) {
        log.debug("[GET /api/v1/skills] Searching skills catalog with keyword: {}", keyword);
        List<SkillResponse> response = userProfileService.searchSkills(keyword);
        return successSingle(response, "Skills retrieved successfully");
    }
}
