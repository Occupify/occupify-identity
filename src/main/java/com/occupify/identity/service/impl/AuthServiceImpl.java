package com.occupify.identity.service.impl;

import com.occupify.identity.dto.request.ChangePasswordRequest;
import com.occupify.identity.dto.request.ForgotPasswordRequest;
import com.occupify.identity.dto.request.LoginRequest;
import com.occupify.identity.dto.request.RegisterRequest;
import com.occupify.identity.dto.request.ResetPasswordRequest;
import com.occupify.identity.dto.request.SendOtpRequest;
import com.occupify.identity.dto.request.VerifyOtpRequest;
import com.occupify.identity.dto.response.AuthResponse;
import com.occupify.identity.dto.response.RegisterResponse;
import com.occupify.identity.dto.response.TokenRefreshResponse;
import com.occupify.identity.dto.response.UserSummaryResponse;
import com.occupify.identity.dto.response.VerifyOtpResponse;
import com.occupify.identity.entity.User;
import com.occupify.identity.enums.OtpType;
import com.occupify.identity.enums.UserStatus;
import com.occupify.identity.exception.AuthErrorCode;
import com.occupify.identity.exception.AuthException;
import com.occupify.identity.jwt.JwtUtils;
import com.occupify.identity.repository.UserRepository;
import com.occupify.identity.service.AuthService;
import com.occupify.identity.service.OtpService;
import com.occupify.identity.service.SessionService;
import com.occupify.identity.service.client.NotificationClient;
import com.occupify.identity.util.EmailUtil;
import com.occupify.identity.util.PasswordUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final SessionService sessionService;
    private final OtpService otpService;
    private final NotificationClient notificationClient;

    public AuthServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtUtils jwtUtils,
            SessionService sessionService,
            OtpService otpService,
            NotificationClient notificationClient) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.sessionService = sessionService;
        this.otpService = otpService;
        this.notificationClient = notificationClient;
    }

    @Override
    @Transactional
    public Mono<RegisterResponse> register(RegisterRequest request) {
        validateEmailAndPassword(request.email(), request.password());
        String normalizedEmail = EmailUtil.normalize(request.email());

        return userRepository.findByEmail(normalizedEmail)
                .flatMap(existingUser -> handleExistingUserRegistration(existingUser, request.password()))
                .switchIfEmpty(Mono.defer(() -> handleNewUserRegistration(normalizedEmail, request.password())));
    }

    @Override
    public Mono<Void> sendOtp(SendOtpRequest request) {
        EmailUtil.validate(request.email());
        String normalizedEmail = EmailUtil.normalize(request.email());
        OtpType type = request.type() != null ? request.type() : OtpType.FORGOT_PASSWORD;

        return type == OtpType.REGISTER
                ? sendRegisterOtp(normalizedEmail)
                : sendForgotPasswordOtp(normalizedEmail);
    }

    @Override
    public Mono<Void> resendOtp(SendOtpRequest request) {
        return sendOtp(request);
    }

    @Override
    @Transactional
    public Mono<Object> verifyOtp(VerifyOtpRequest request) {
        EmailUtil.validate(request.email());
        String normalizedEmail = EmailUtil.normalize(request.email());
        OtpType type = request.type() != null ? request.type() : OtpType.FORGOT_PASSWORD;

        return type == OtpType.REGISTER
                ? verifyRegisterOtp(normalizedEmail, request.otpCode())
                : verifyForgotPasswordOtp(normalizedEmail, request.otpCode());
    }

    @Override
    public Mono<AuthResponse> login(LoginRequest request) {
        validateEmailAndPassword(request.email(), request.password());
        String normalizedEmail = EmailUtil.normalize(request.email());

        return userRepository.findByEmail(normalizedEmail)
                .switchIfEmpty(Mono.error(new AuthException(AuthErrorCode.AUTH_003)))
                .flatMap(user -> authenticateUser(user, request.password()))
                .flatMap(this::rotateUserSessionAndRespond);
    }

    @Override
    public Mono<TokenRefreshResponse> refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return Mono.error(new AuthException(AuthErrorCode.AUTH_004));
        }

        if (!jwtUtils.validateToken(refreshToken)) {
            return Mono.error(new AuthException(AuthErrorCode.AUTH_005));
        }

        return sessionService.isValidSession(refreshToken)
                .flatMap(isValid -> {
                    if (!Boolean.TRUE.equals(isValid)) {
                        return Mono.error(new AuthException(AuthErrorCode.AUTH_006));
                    }
                    return resolveUserForTokenRefresh(refreshToken);
                })
                .map(user -> {
                    String newAccessToken = jwtUtils.generateAccessToken(
                            user.getEmail(),
                            user.getId().toString(),
                            user.getRole()
                    );
                    return new TokenRefreshResponse(newAccessToken, refreshToken);
                });
    }

    @Override
    public Mono<Void> signOut(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return Mono.error(new AuthException(AuthErrorCode.AUTH_004));
        }

        return sessionService.revokeSession(refreshToken)
                .then();
    }

    @Override
    public Mono<Void> forgotPassword(ForgotPasswordRequest request) {
        return sendOtp(new SendOtpRequest(request.email(), OtpType.FORGOT_PASSWORD));
    }

    @Override
    @Transactional
    public Mono<Void> resetPassword(ResetPasswordRequest request) {
        validateEmailAndPassword(request.email(), request.newPassword());
        String normalizedEmail = EmailUtil.normalize(request.email());

        if (request.resetToken() != null && !request.resetToken().isBlank()) {
            return resetPasswordWithToken(normalizedEmail, request.resetToken(), request.newPassword());
        }
        if (request.otpCode() != null && !request.otpCode().isBlank()) {
            return resetPasswordWithOtp(normalizedEmail, request.otpCode(), request.newPassword());
        }
        return Mono.error(new AuthException(AuthErrorCode.AUTH_016));
    }

    @Override
    @Transactional
    public Mono<Void> changePassword(String userEmail, ChangePasswordRequest request) {
        if (userEmail == null || userEmail.isBlank()) {
            return Mono.error(new AuthException(AuthErrorCode.AUTH_003, "Unauthenticated user context"));
        }
        PasswordUtil.validatePasswordChange(request.currentPassword(), request.newPassword());
        String normalizedEmail = EmailUtil.normalize(userEmail);

        return userRepository.findByEmail(normalizedEmail)
                .switchIfEmpty(Mono.error(new AuthException(AuthErrorCode.USER_001)))
                .flatMap(user -> verifyCurrentPassword(user, request.currentPassword()))
                .flatMap(user -> updatePasswordAndInvalidateSessions(normalizedEmail, request.newPassword()));
    }

    private Mono<RegisterResponse> handleExistingUserRegistration(User existingUser, String newPassword) {
        if (UserStatus.ACTIVE.name().equalsIgnoreCase(existingUser.getStatus())) {
            return Mono.error(new AuthException(AuthErrorCode.AUTH_001));
        }
        if (UserStatus.BANNED.name().equalsIgnoreCase(existingUser.getStatus())) {
            return Mono.error(new AuthException(AuthErrorCode.USER_008));
        }
        String encodedPassword = passwordEncoder.encode(newPassword);
        return userRepository.updatePasswordByEmail(existingUser.getEmail(), encodedPassword)
                .then(otpService.generateAndStoreOtp(existingUser.getEmail(), OtpType.REGISTER))
                .flatMap(rawOtp -> notificationClient.sendRegistrationOtp(existingUser.getEmail(), rawOtp))
                .thenReturn(new RegisterResponse(existingUser.getId(), existingUser.getEmail(), existingUser.getStatus()));
    }

    private Mono<RegisterResponse> handleNewUserRegistration(String email, String rawPassword) {
        String encodedPassword = passwordEncoder.encode(rawPassword);
        User newUser = User.createInactive(email, encodedPassword);
        return userRepository.save(newUser)
                .flatMap(savedUser -> otpService.generateAndStoreOtp(email, OtpType.REGISTER)
                        .flatMap(rawOtp -> notificationClient.sendRegistrationOtp(email, rawOtp))
                        .thenReturn(new RegisterResponse(savedUser.getId(), savedUser.getEmail(), savedUser.getStatus())));
    }

    private Mono<Void> sendRegisterOtp(String email) {
        return userRepository.findByEmail(email)
                .switchIfEmpty(Mono.error(new AuthException(AuthErrorCode.AUTH_012)))
                .flatMap(user -> {
                    if (UserStatus.ACTIVE.name().equalsIgnoreCase(user.getStatus())) {
                        return Mono.error(new AuthException(AuthErrorCode.AUTH_015));
                    }
                    if (UserStatus.BANNED.name().equalsIgnoreCase(user.getStatus())) {
                        return Mono.error(new AuthException(AuthErrorCode.USER_008));
                    }
                    return otpService.generateAndStoreOtp(email, OtpType.REGISTER);
                })
                .flatMap(rawOtp -> notificationClient.sendRegistrationOtp(email, rawOtp));
    }

    private Mono<Void> sendForgotPasswordOtp(String email) {
        return userRepository.findByEmail(email)
                .switchIfEmpty(Mono.error(new AuthException(AuthErrorCode.AUTH_012)))
                .flatMap(user -> {
                    if (UserStatus.INACTIVE.name().equalsIgnoreCase(user.getStatus())) {
                        return Mono.error(new AuthException(AuthErrorCode.USER_007));
                    }
                    if (UserStatus.BANNED.name().equalsIgnoreCase(user.getStatus())) {
                        return Mono.error(new AuthException(AuthErrorCode.USER_008));
                    }
                    return otpService.generateAndStoreOtp(email, OtpType.FORGOT_PASSWORD);
                })
                .flatMap(rawOtp -> notificationClient.sendPasswordResetOtp(email, rawOtp));
    }

    private Mono<Object> verifyRegisterOtp(String email, String otpCode) {
        return userRepository.findByEmail(email)
                .switchIfEmpty(Mono.error(new AuthException(AuthErrorCode.AUTH_012)))
                .flatMap(user -> {
                    if (UserStatus.ACTIVE.name().equalsIgnoreCase(user.getStatus())) {
                        return Mono.error(new AuthException(AuthErrorCode.AUTH_015));
                    }
                    if (UserStatus.BANNED.name().equalsIgnoreCase(user.getStatus())) {
                        return Mono.error(new AuthException(AuthErrorCode.USER_008));
                    }
                    return otpService.verifyOtp(email, otpCode, OtpType.REGISTER)
                            .then(userRepository.updateStatusByEmail(email, UserStatus.ACTIVE.name()))
                            .then(Mono.defer(() -> {
                                user.setStatus(UserStatus.ACTIVE.name());
                                return generateAuthResponse(user).map(auth -> (Object) auth);
                            }));
                });
    }

    private Mono<Object> verifyForgotPasswordOtp(String email, String otpCode) {
        return userRepository.findByEmail(email)
                .switchIfEmpty(Mono.error(new AuthException(AuthErrorCode.AUTH_012)))
                .flatMap(user -> {
                    if (UserStatus.INACTIVE.name().equalsIgnoreCase(user.getStatus())) {
                        return Mono.error(new AuthException(AuthErrorCode.USER_007));
                    }
                    if (UserStatus.BANNED.name().equalsIgnoreCase(user.getStatus())) {
                        return Mono.error(new AuthException(AuthErrorCode.USER_008));
                    }
                    return otpService.verifyOtp(email, otpCode, OtpType.FORGOT_PASSWORD)
                            .then(otpService.createPasswordResetToken(email))
                            .map(resetToken -> (Object) new VerifyOtpResponse(resetToken));
                });
    }

    private Mono<Void> resetPasswordWithToken(String email, String resetToken, String newPassword) {
        return otpService.validatePasswordResetToken(email, resetToken)
                .then(userRepository.findByEmail(email))
                .switchIfEmpty(Mono.error(new AuthException(AuthErrorCode.AUTH_012)))
                .flatMap(user -> updatePasswordAndInvalidateSessions(email, newPassword))
                .then(otpService.deletePasswordResetToken(resetToken));
    }

    private Mono<Void> resetPasswordWithOtp(String email, String otpCode, String newPassword) {
        return otpService.verifyOtp(email, otpCode, OtpType.FORGOT_PASSWORD)
                .then(userRepository.findByEmail(email))
                .switchIfEmpty(Mono.error(new AuthException(AuthErrorCode.AUTH_012)))
                .flatMap(user -> updatePasswordAndInvalidateSessions(email, newPassword));
    }

    private Mono<AuthResponse> generateAuthResponse(User user) {
        String accessToken = jwtUtils.generateAccessToken(user.getEmail(), user.getId().toString(), user.getRole());
        String refreshToken = jwtUtils.generateRefreshToken(user.getEmail());

        return sessionService.saveSession(refreshToken, user.getId(), user.getEmail())
                .thenReturn(new AuthResponse(accessToken, refreshToken, mapToUserSummary(user)));
    }

    private Mono<User> authenticateUser(User user, String rawPassword) {
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            return Mono.error(new AuthException(AuthErrorCode.AUTH_003));
        }
        return validateUserStatus(user);
    }

    private Mono<User> validateUserStatus(User user) {
        if (UserStatus.INACTIVE.name().equalsIgnoreCase(user.getStatus())) {
            return Mono.error(new AuthException(AuthErrorCode.USER_007));
        }
        if (UserStatus.BANNED.name().equalsIgnoreCase(user.getStatus())) {
            return Mono.error(new AuthException(AuthErrorCode.USER_008));
        }
        return Mono.just(user);
    }

    private Mono<AuthResponse> rotateUserSessionAndRespond(User user) {
        return sessionService.revokeAllUserSessions(user.getEmail())
                .then(generateAuthResponse(user));
    }

    private Mono<User> resolveUserForTokenRefresh(String refreshToken) {
        String email = jwtUtils.extractEmail(refreshToken);
        return userRepository.findByEmail(email)
                .switchIfEmpty(Mono.error(new AuthException(AuthErrorCode.AUTH_003)))
                .flatMap(this::validateUserStatus);
    }

    private Mono<User> verifyCurrentPassword(User user, String currentPassword) {
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            return Mono.error(new AuthException(AuthErrorCode.AUTH_013));
        }
        return Mono.just(user);
    }

    private Mono<Void> updatePasswordAndInvalidateSessions(String email, String newPassword) {
        String encodedPassword = passwordEncoder.encode(newPassword);
        return userRepository.updatePasswordByEmail(email, encodedPassword)
                .then(sessionService.revokeAllUserSessions(email))
                .then();
    }

    private UserSummaryResponse mapToUserSummary(User user) {
        return new UserSummaryResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt()
        );
    }

    private void validateEmailAndPassword(String email, String password) {
        EmailUtil.validate(email);
        PasswordUtil.validate(password);
    }
}
