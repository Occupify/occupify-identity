package com.occupify.identity.controller.user;

import com.occupify.identity.config.OpenApiConfig;
import com.occupify.identity.controller.AbstractBaseController;
import com.occupify.identity.dto.base.SingleResponse;
import com.occupify.identity.dto.base.SuccessResponse;
import com.occupify.identity.dto.request.user.UpdateUserProfileRequest;
import com.occupify.identity.dto.response.user.AvatarResponse;
import com.occupify.identity.dto.response.user.UserDetailResponse;
import com.occupify.identity.dto.response.user.UserProfileResponse;
import com.occupify.identity.security.JwtUtils;
import com.occupify.identity.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/users")
@Tag(name = "User Account", description = "Endpoints for user profile and account self-management")
public class UserController extends AbstractBaseController {

        private final UserService userService;
        private final JwtUtils jwtUtils;

        public UserController(UserService userService, JwtUtils jwtUtils) {
                this.userService = userService;
                this.jwtUtils = jwtUtils;
        }

        @GetMapping("/me")
        @Operation(summary = "Get current user profile", description = "Retrieves the basic profile of the authenticated user", security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
        public SingleResponse<UserProfileResponse> getMyProfile(
                        @Parameter(description = "Injected user ID from Gateway", hidden = true) @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
                        HttpServletRequest request) {
                UUID currentUserId = resolveUserId(headerUserId, request, jwtUtils);
                log.info("[GET /users/me] Fetching profile for user: {}", currentUserId);
                UserProfileResponse response = userService.getMyProfile(currentUserId);
                return successSingle(response, "User profile retrieved successfully");
        }

        @GetMapping("/me/detail")
        @Operation(summary = "Get current user full 360-degree profile detail", description = "Retrieves comprehensive user profile including experiences, educations, qualifications, and skills", security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
        public SingleResponse<UserDetailResponse> getMyProfileDetail(
                        @Parameter(description = "Injected user ID from Gateway", hidden = true) @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
                        HttpServletRequest request) {
                UUID currentUserId = resolveUserId(headerUserId, request, jwtUtils);
                log.info("[GET /users/me/detail] Fetching 360-degree profile for user: {}", currentUserId);
                UserDetailResponse response = userService.getMyProfileDetail(currentUserId);
                return successSingle(response, "User full profile detail retrieved successfully");
        }

        @GetMapping("/{id}/detail")
        @Operation(summary = "Get public user profile detail by ID", description = "Retrieves public candidate detail for external review")
        public SingleResponse<UserDetailResponse> getPublicUserDetail(@PathVariable("id") UUID id) {
                log.info("[GET /users/{}/detail] Fetching public candidate detail", id);
                UserDetailResponse response = userService.getPublicUserDetail(id);
                return successSingle(response, "Public user profile detail retrieved successfully");
        }

        @PutMapping("/me")
        @Operation(summary = "Update current user profile", description = "Updates personal details such as full name, headline, bio, and phone number", security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
        public SingleResponse<UserProfileResponse> updateMyProfile(
                        @Parameter(description = "Injected user ID from Gateway", hidden = true) @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
                        @Valid @RequestBody UpdateUserProfileRequest updateRequest,
                        HttpServletRequest request) {
                UUID currentUserId = resolveUserId(headerUserId, request, jwtUtils);
                log.info("[PUT /users/me] Updating profile for user: {}", currentUserId);
                UserProfileResponse response = userService.updateMyProfile(currentUserId, updateRequest);
                return successSingle(response, "User profile updated successfully");
        }

        @DeleteMapping("/me")
        @Operation(summary = "Self-delete current user account (Soft Delete)", description = "Soft-deletes the user account and marks status as DELETED", security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
        public SuccessResponse deleteMyAccount(
                        @Parameter(description = "Injected user ID from Gateway", hidden = true) @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
                        HttpServletRequest request) {
                UUID currentUserId = resolveUserId(headerUserId, request, jwtUtils);
                log.warn("[DELETE /users/me] Self-deleting account for user: {}", currentUserId);
                userService.deleteMyAccount(currentUserId);
                return success("User account deleted successfully");
        }

        @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        @Operation(summary = "Upload current user avatar", description = "Uploads a new avatar image (JPEG, PNG, WEBP, max 5MB) to S3/OCI storage", security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
        public SingleResponse<AvatarResponse> uploadMyAvatar(
                        @Parameter(description = "Injected user ID from Gateway", hidden = true) @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
                        @RequestParam("file") MultipartFile file,
                        HttpServletRequest request) {
                UUID currentUserId = resolveUserId(headerUserId, request, jwtUtils);
                log.info("[POST /users/me/avatar] Uploading avatar for user: {}", currentUserId);
                AvatarResponse response = userService.uploadMyAvatar(currentUserId, file);
                return successSingle(response, "Avatar uploaded successfully");
        }

        @DeleteMapping("/me/avatar")
        @Operation(summary = "Delete current user avatar", description = "Deletes avatar image from storage and resets avatar URL to null", security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
        public SuccessResponse deleteMyAvatar(
                        @Parameter(description = "Injected user ID from Gateway", hidden = true) @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
                        HttpServletRequest request) {
                UUID currentUserId = resolveUserId(headerUserId, request, jwtUtils);
                log.info("[DELETE /users/me/avatar] Deleting avatar for user: {}", currentUserId);
                userService.deleteMyAvatar(currentUserId);
                return success("Avatar deleted successfully");
        }
}
