package com.occupify.identity.service.user.impl;

import com.occupify.identity.constant.StorageConstants;
import com.occupify.identity.dto.request.user.UpdateUserProfileRequest;
import com.occupify.identity.dto.response.user.AvatarResponse;
import com.occupify.identity.dto.response.user.EducationItemResponse;
import com.occupify.identity.dto.response.user.ExperienceItemResponse;
import com.occupify.identity.dto.response.user.QualificationItemResponse;
import com.occupify.identity.dto.response.user.UserDetailResponse;
import com.occupify.identity.dto.response.user.UserProfileResponse;
import com.occupify.identity.dto.response.user.UserSkillItemResponse;
import com.occupify.identity.entity.User;
import com.occupify.identity.enums.UserStatus;
import com.occupify.identity.exception.user.UserErrorCode;
import com.occupify.identity.exception.user.UserException;
import com.occupify.identity.mapper.UserMapper;
import com.occupify.identity.mapper.UserProfileMapper;
import com.occupify.identity.repository.EducationRepository;
import com.occupify.identity.repository.ExperienceRepository;
import com.occupify.identity.repository.QualificationRepository;
import com.occupify.identity.repository.SkillUserRepository;
import com.occupify.identity.repository.UserRepository;
import com.occupify.identity.service.storage.StorageService;
import com.occupify.identity.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ExperienceRepository experienceRepository;
    private final EducationRepository educationRepository;
    private final QualificationRepository qualificationRepository;
    private final SkillUserRepository skillUserRepository;
    private final com.occupify.identity.service.storage.StorageService storageService;
    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;

    @Override
    public UserProfileResponse getMyProfile(UUID currentUserId) {
        log.debug("Fetching user profile for userId: {}", currentUserId);
        User user = findActiveUserOrThrow(currentUserId);
        return userMapper.toUserProfileResponse(user);
    }

    @Override
    public UserDetailResponse getMyProfileDetail(UUID currentUserId) {
        log.debug("Fetching 360-degree user detail for userId: {}", currentUserId);
        User user = findActiveUserOrThrow(currentUserId);
        return buildUserDetailResponse(user);
    }

    @Override
    public UserDetailResponse getPublicUserDetail(UUID targetUserId) {
        log.debug("Fetching public profile detail for targetUserId: {}", targetUserId);
        User user = findActiveUserOrThrow(targetUserId);
        return buildUserDetailResponse(user);
    }

    @Override
    @Transactional
    public UserProfileResponse updateMyProfile(UUID currentUserId, UpdateUserProfileRequest request) {
        log.info("Updating profile for userId: {}", currentUserId);
        User user = findActiveUserOrThrow(currentUserId);

        user.setFullName(request.fullName());
        user.setHeadline(request.headline());
        user.setBio(request.bio());
        user.setPhoneNumber(request.phoneNumber());

        User updatedUser = userRepository.save(user);
        log.info("Successfully updated profile for userId: {}", currentUserId);
        return userMapper.toUserProfileResponse(updatedUser);
    }

    @Override
    @Transactional
    public void deleteMyAccount(UUID currentUserId) {
        log.warn("Soft deleting account for userId: {}", currentUserId);
        User user = findActiveUserOrThrow(currentUserId);

        user.setStatus(UserStatus.DELETED.name());
        userRepository.save(user);
        log.info("Account soft-deleted successfully for userId: {}", currentUserId);
    }

    @Override
    @Transactional
    public AvatarResponse uploadMyAvatar(UUID currentUserId, MultipartFile file) {
        log.info("Uploading avatar for userId: {}", currentUserId);
        User user = findActiveUserOrThrow(currentUserId);

        String oldAvatarUrl = user.getAvatarUrl();
        String newAvatarUrl = storageService.uploadImage(StorageConstants.getAvatarPath(currentUserId), file);

        user.setAvatarUrl(newAvatarUrl);
        userRepository.save(user);
        log.info("Successfully updated avatarUrl for userId: {} to {}", currentUserId, newAvatarUrl);

        if (oldAvatarUrl != null && !oldAvatarUrl.isBlank()) {
            try {
                storageService.deleteFile(oldAvatarUrl);
            } catch (Exception e) {
                log.warn("Failed to delete obsolete avatar file for userId {}: {}", currentUserId, oldAvatarUrl, e);
            }
        }

        return new AvatarResponse(newAvatarUrl);
    }

    @Override
    @Transactional
    public void deleteMyAvatar(UUID currentUserId) {
        log.info("Deleting avatar for userId: {}", currentUserId);
        User user = findActiveUserOrThrow(currentUserId);

        String currentAvatarUrl = user.getAvatarUrl();
        if (currentAvatarUrl != null && !currentAvatarUrl.isBlank()) {
            try {
                storageService.deleteFile(currentAvatarUrl);
            } catch (Exception e) {
                log.warn("Failed to delete obsolete avatar file for userId {}: {}", currentUserId, currentAvatarUrl, e);
            }
            user.setAvatarUrl(null);
            userRepository.save(user);
            log.info("Successfully cleared avatar for userId: {}", currentUserId);
        }
    }

    private User findActiveUserOrThrow(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(
                        () -> new UserException(UserErrorCode.USER_NOT_FOUND, "User not found with id: " + userId));

        if (UserStatus.DELETED.name().equalsIgnoreCase(user.getStatus())) {
            throw new UserException(UserErrorCode.USER_ALREADY_DELETED, "User account has been deleted");
        }
        return user;
    }

    private UserDetailResponse buildUserDetailResponse(User user) {
        UUID userId = user.getId();
        UserProfileResponse profileResponse = userMapper.toUserProfileResponse(user);

        List<ExperienceItemResponse> experiences = experienceRepository.findByUserIdOrderByStartDateDesc(userId)
                .stream()
                .map(userProfileMapper::toExperienceResponse)
                .toList();

        List<EducationItemResponse> educations = educationRepository.findByUserIdOrderByStartDateDesc(userId)
                .stream()
                .map(userProfileMapper::toEducationResponse)
                .toList();

        List<QualificationItemResponse> qualifications = qualificationRepository
                .findByUserIdOrderByIssueDateDesc(userId)
                .stream()
                .map(userProfileMapper::toQualificationResponse)
                .toList();

        List<UserSkillItemResponse> skills = skillUserRepository.findByIdUserId(userId)
                .stream()
                .map(userProfileMapper::toUserSkillResponse)
                .toList();

        return new UserDetailResponse(
                profileResponse,
                experiences,
                educations,
                qualifications,
                skills);
    }
}
