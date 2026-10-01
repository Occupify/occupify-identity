package com.occupify.identity.util;

import com.occupify.identity.exception.AuthErrorCode;
import com.occupify.identity.exception.AuthException;

import java.util.regex.Pattern;

public final class EmailUtil {

    private static final int MAX_EMAIL_LENGTH = 254;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    private EmailUtil() {
        // Utility class
    }

    public static boolean isValid(String email) {
        if (email == null) {
            return false;
        }
        String trimmed = email.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_EMAIL_LENGTH) {
            return false;
        }
        return EMAIL_PATTERN.matcher(trimmed).matches();
    }

    public static String normalize(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase();
    }

    public static void validate(String email) {
        if (!isValid(email)) {
            throw new AuthException(AuthErrorCode.AUTH_000);
        }
    }

    public static String mask(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        int atIndex = email.indexOf('@');
        String name = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        String maskedName = name.length() <= 2 ? name.charAt(0) + "***" : name.charAt(0) + "***" + name.charAt(name.length() - 1);
        return maskedName + domain;
    }
}
