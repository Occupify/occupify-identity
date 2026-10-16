package com.occupify.identity.service.user;

import com.occupify.identity.dto.request.user.UpdateUserProfileRequest;
import com.occupify.identity.dto.response.user.AvatarResponse;
import com.occupify.identity.dto.response.user.UserDetailResponse;
import com.occupify.identity.dto.response.user.UserProfileResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface UserService {
    UserProfileResponse getMyProfile(UUID currentUserId);

    UserDetailResponse getMyProfileDetail(UUID currentUserId);

    UserDetailResponse getPublicUserDetail(UUID targetUserId);

    UserProfileResponse updateMyProfile(UUID currentUserId, UpdateUserProfileRequest request);

    void deleteMyAccount(UUID currentUserId);

    AvatarResponse uploadMyAvatar(UUID currentUserId, MultipartFile file);

    void deleteMyAvatar(UUID currentUserId);
}
