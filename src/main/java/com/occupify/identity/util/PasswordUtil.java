package com.occupify.identity.util;

import com.occupify.identity.exception.AuthErrorCode;
import com.occupify.identity.exception.AuthException;

public final class PasswordUtil {

    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final int MAX_PASSWORD_LENGTH = 100;

    private PasswordUtil() {
        // Utility class
    }

    public static boolean isValid(String password) {
        if (password == null || password.isBlank()) {
            return false;
        }
        return password.length() >= MIN_PASSWORD_LENGTH && password.length() <= MAX_PASSWORD_LENGTH;
    }

    public static void validate(String password) {
        if (!isValid(password)) {
            throw new AuthException(AuthErrorCode.AUTH_002);
        }
    }

    public static void validatePasswordChange(String currentPassword, String newPassword) {
        validate(newPassword);
        if (currentPassword != null && currentPassword.equals(newPassword)) {
            throw new AuthException(AuthErrorCode.AUTH_014, "New password cannot be identical to current password");
        }
    }
}
