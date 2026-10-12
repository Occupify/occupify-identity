package com.occupify.identity.service.user;

import com.occupify.identity.dto.request.user.UpdateUserProfileRequest;
import com.occupify.identity.dto.response.user.AvatarResponse;
import com.occupify.identity.dto.response.user.UserDetailResponse;
import com.occupify.identity.dto.response.user.UserProfileResponse;

import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

public interface UserService {
    UserProfileResponse getMyProfile(UUID currentUserId);

    UserDetailResponse getMyProfileDetail(UUID currentUserId);

    UserDetailResponse getPublicUserDetail(UUID targetUserId);

    UserProfileResponse updateMyProfile(UUID currentUserId, UpdateUserProfileRequest request);

    void deleteMyAccount(UUID currentUserId);

    AvatarResponse uploadMyAvatar(UUID currentUserId, MultipartFile file);

    AvatarResponse getMyAvatar(UUID currentUserId);

    void deleteMyAvatar(UUID currentUserId);
}
