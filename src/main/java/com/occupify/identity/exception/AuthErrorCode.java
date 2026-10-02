package com.occupify.identity.exception;

import org.springframework.http.HttpStatus;

public enum AuthErrorCode {
    AUTH_000("AUTH_000", HttpStatus.BAD_REQUEST, "Invalid email format"),
    AUTH_001("AUTH_001", HttpStatus.CONFLICT, "Email already exists in the system"),
    AUTH_002("AUTH_002", HttpStatus.BAD_REQUEST, "Password must be at least 8 characters long"),
    AUTH_003("AUTH_003", HttpStatus.UNAUTHORIZED, "Invalid email or password"),
    AUTH_004("AUTH_004", HttpStatus.BAD_REQUEST, "Refresh token is required"),
    AUTH_005("AUTH_005", HttpStatus.UNAUTHORIZED, "Invalid refresh token"),
    AUTH_006("AUTH_006", HttpStatus.UNAUTHORIZED, "Refresh token has expired or been revoked"),
    AUTH_007("AUTH_007", HttpStatus.TOO_MANY_REQUESTS, "Please wait before requesting a new OTP"),
    AUTH_008("AUTH_008", HttpStatus.BAD_REQUEST, "OTP has expired or does not exist"),
    AUTH_009("AUTH_009", HttpStatus.BAD_REQUEST, "Invalid OTP code"),
    AUTH_010("AUTH_010", HttpStatus.TOO_MANY_REQUESTS, "Maximum OTP verification attempts exceeded"),
    AUTH_011("AUTH_011", HttpStatus.INTERNAL_SERVER_ERROR, "Failed to dispatch notification email"),
    AUTH_012("AUTH_012", HttpStatus.NOT_FOUND, "User not found"),
    AUTH_013("AUTH_013", HttpStatus.BAD_REQUEST, "Current password does not match"),
    AUTH_014("AUTH_014", HttpStatus.BAD_REQUEST, "New password cannot be identical to current password"),
    AUTH_015("AUTH_015", HttpStatus.BAD_REQUEST, "Account is already activated"),
    AUTH_016("AUTH_016", HttpStatus.BAD_REQUEST, "Invalid or expired reset token"),
    USER_001("USER_001", HttpStatus.NOT_FOUND, "User account not found"),
    USER_007("USER_007", HttpStatus.FORBIDDEN, "User account is inactive or locked"),
    USER_008("USER_008", HttpStatus.FORBIDDEN, "User account has been banned");

    private final String code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;

    AuthErrorCode(String code, HttpStatus httpStatus, String defaultMessage) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
