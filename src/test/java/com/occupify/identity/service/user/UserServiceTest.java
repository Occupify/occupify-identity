package com.occupify.identity.service.user;

import com.occupify.identity.constant.StorageConstants;
import com.occupify.identity.dto.request.user.UpdateUserProfileRequest;
import com.occupify.identity.dto.response.user.AvatarResponse;
import com.occupify.identity.dto.response.user.UserDetailResponse;
import com.occupify.identity.dto.response.user.UserProfileResponse;
import com.occupify.identity.entity.User;
import com.occupify.identity.enums.UserRole;
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
import com.occupify.identity.service.user.impl.UserServiceImpl;
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
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl Unit Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ExperienceRepository experienceRepository;

    @Mock
    private EducationRepository educationRepository;

    @Mock
    private QualificationRepository qualificationRepository;

    @Mock
    private SkillUserRepository skillUserRepository;

    @Mock
    private StorageService storageService;

    @Spy
    private UserMapper userMapper = new UserMapper();

    @Spy
    private UserProfileMapper userProfileMapper = new UserProfileMapper();

    @InjectMocks
    private UserServiceImpl userService;

    private UUID userId;
    private User activeUser;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        activeUser = new User(
                userId,
                "dev@occupify.work",
                "hashedPass",
                UserRole.USER.name(),
                UserStatus.ACTIVE.name(),
                Instant.now(),
                Instant.now()
        );
        activeUser.setFullName("Alex Hunter");
        activeUser.setHeadline("Senior Engineer");
        activeUser.setAvatarUrl("https://avatar.url");
        activeUser.setBio("Bio summary");
        activeUser.setPhoneNumber("+1234567890");
    }

    @Test
    @DisplayName("getMyProfile -> returns UserProfileResponse when user is active")
    void getMyProfile_success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        UserProfileResponse response = userService.getMyProfile(userId);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(userId);
        assertThat(response.email()).isEqualTo("dev@occupify.work");
        assertThat(response.fullName()).isEqualTo("Alex Hunter");
    }

    @Test
    @DisplayName("getMyProfile -> throws USER_NOT_FOUND when user does not exist")
    void getMyProfile_notFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getMyProfile(userId))
                .isInstanceOf(UserException.class)
                .extracting("errorCode")
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("getMyProfile -> throws USER_ALREADY_DELETED when user status is DELETED")
    void getMyProfile_deletedUser() {
        activeUser.setStatus(UserStatus.DELETED.name());
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        assertThatThrownBy(() -> userService.getMyProfile(userId))
                .isInstanceOf(UserException.class)
                .extracting("errorCode")
                .isEqualTo(UserErrorCode.USER_ALREADY_DELETED);
    }

    @Test
    @DisplayName("getMyProfileDetail -> returns aggregated 360-degree profile")
    void getMyProfileDetail_success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(experienceRepository.findByUserIdOrderByStartDateDesc(userId)).thenReturn(Collections.emptyList());
        when(educationRepository.findByUserIdOrderByStartDateDesc(userId)).thenReturn(Collections.emptyList());
        when(qualificationRepository.findByUserIdOrderByIssueDateDesc(userId)).thenReturn(Collections.emptyList());
        when(skillUserRepository.findByIdUserId(userId)).thenReturn(Collections.emptyList());

        UserDetailResponse detail = userService.getMyProfileDetail(userId);

        assertThat(detail).isNotNull();
        assertThat(detail.user().id()).isEqualTo(userId);
        assertThat(detail.experiences()).isEmpty();
        assertThat(detail.educations()).isEmpty();
        assertThat(detail.qualifications()).isEmpty();
        assertThat(detail.skills()).isEmpty();
    }

    @Test
    @DisplayName("getPublicUserDetail -> returns public candidate detail")
    void getPublicUserDetail_success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(experienceRepository.findByUserIdOrderByStartDateDesc(userId)).thenReturn(Collections.emptyList());
        when(educationRepository.findByUserIdOrderByStartDateDesc(userId)).thenReturn(Collections.emptyList());
        when(qualificationRepository.findByUserIdOrderByIssueDateDesc(userId)).thenReturn(Collections.emptyList());
        when(skillUserRepository.findByIdUserId(userId)).thenReturn(Collections.emptyList());

        UserDetailResponse detail = userService.getPublicUserDetail(userId);

        assertThat(detail).isNotNull();
        assertThat(detail.user().fullName()).isEqualTo("Alex Hunter");
    }

    @Test
    @DisplayName("updateMyProfile -> successfully mutates and persists user fields")
    void updateMyProfile_success() {
        UpdateUserProfileRequest updateRequest = new UpdateUserProfileRequest(
                "Alex Updated",
                "Principal Architect",
                "New bio",
                "+9876543210"
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileResponse response = userService.updateMyProfile(userId, updateRequest);

        assertThat(response).isNotNull();
        assertThat(response.fullName()).isEqualTo("Alex Updated");
        assertThat(response.headline()).isEqualTo("Principal Architect");
        assertThat(response.bio()).isEqualTo("New bio");
        verify(userRepository).save(activeUser);
    }

    @Test
    @DisplayName("deleteMyAccount -> marks user status as DELETED")
    void deleteMyAccount_success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.deleteMyAccount(userId);

        assertThat(activeUser.getStatus()).isEqualTo(UserStatus.DELETED.name());
        verify(userRepository).save(activeUser);
    }

    @Test
    @DisplayName("uploadMyAvatar -> when user has no prior avatar -> uploads to storage and saves new URL")
    void uploadMyAvatar_whenNoPriorAvatar_shouldUploadAndSave() {
        activeUser.setAvatarUrl(null);
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", "bytes".getBytes());
        String expectedUrl = "https://objectstorage.ap-singapore-1.oraclecloud.com/.../new-avatar.png";
        when(storageService.uploadImage(eq(StorageConstants.getAvatarPath(userId)), eq(file))).thenReturn(expectedUrl);

        AvatarResponse response = userService.uploadMyAvatar(userId, file);

        assertThat(response).isNotNull();
        assertThat(response.avatarUrl()).isEqualTo(expectedUrl);
        assertThat(activeUser.getAvatarUrl()).isEqualTo(expectedUrl);
        verify(storageService).uploadImage(StorageConstants.getAvatarPath(userId), file);
        verify(userRepository).save(activeUser);
    }

    @Test
    @DisplayName("uploadMyAvatar -> when user already has old avatar -> uploads new and triggers delete of old avatar")
    void uploadMyAvatar_whenOldAvatarExists_shouldUploadAndCleanOldAvatar() {
        String oldUrl = "https://objectstorage.ap-singapore-1.oraclecloud.com/.../old-avatar.png";
        activeUser.setAvatarUrl(oldUrl);
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MockMultipartFile file = new MockMultipartFile("file", "new-avatar.jpg", "image/jpeg", "bytes".getBytes());
        String newUrl = "https://objectstorage.ap-singapore-1.oraclecloud.com/.../new-avatar.jpg";
        when(storageService.uploadImage(eq(StorageConstants.getAvatarPath(userId)), eq(file))).thenReturn(newUrl);

        AvatarResponse response = userService.uploadMyAvatar(userId, file);

        assertThat(response.avatarUrl()).isEqualTo(newUrl);
        assertThat(activeUser.getAvatarUrl()).isEqualTo(newUrl);
        verify(storageService).uploadImage(StorageConstants.getAvatarPath(userId), file);
        verify(storageService).deleteFile(oldUrl);
        verify(userRepository).save(activeUser);
    }

    @Test
    @DisplayName("deleteMyAvatar -> when user has avatar -> deletes from storage and sets avatarUrl null")
    void deleteMyAvatar_whenUserHasAvatar_shouldDeleteFileAndResetUrl() {
        String currentUrl = "https://objectstorage.../current-avatar.png";
        activeUser.setAvatarUrl(currentUrl);
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.deleteMyAvatar(userId);

        assertThat(activeUser.getAvatarUrl()).isNull();
        verify(storageService).deleteFile(currentUrl);
        verify(userRepository).save(activeUser);
    }
}
