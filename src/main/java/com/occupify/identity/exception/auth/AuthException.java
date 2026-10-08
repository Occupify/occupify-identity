package com.occupify.identity.exception.auth;

import com.occupify.identity.exception.BaseException;
import lombok.Getter;

@Getter
public class AuthException extends BaseException {

    private final AuthErrorCode errorCode;

    public AuthException(AuthErrorCode errorCode) {
        super(errorCode.getCode(), errorCode.getMessage(), errorCode.getHttpStatus());
        this.errorCode = errorCode;
    }

    public AuthException(AuthErrorCode errorCode, String customMessage) {
        super(errorCode.getCode(), customMessage != null ? customMessage : errorCode.getMessage(), errorCode.getHttpStatus());
        this.errorCode = errorCode;
    }
}
