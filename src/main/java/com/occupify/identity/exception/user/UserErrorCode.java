package com.occupify.identity.exception.user;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode {

    USER_NOT_FOUND("USER_001", "User account not found", HttpStatus.NOT_FOUND),
    USER_ALREADY_DELETED("USER_002", "User account is already deleted", HttpStatus.GONE),
    USER_INACTIVE_OR_LOCKED("USER_007", "User account is inactive or locked", HttpStatus.FORBIDDEN),
    USER_BANNED("USER_008", "User account has been banned", HttpStatus.FORBIDDEN);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
