package com.occupify.identity.exception.auth;

import com.occupify.identity.exception.BaseException;
import lombok.Getter;

@Getter
public class AuthException extends BaseException {

    private final AuthErrorCode errorCode;

    public AuthException(AuthErrorCode errorCode) {
        super(errorCode.getCode(), errorCode.getDefaultMessage(), errorCode.getHttpStatus());
        this.errorCode = errorCode;
    }

    public AuthException(AuthErrorCode errorCode, String customMessage) {
        super(errorCode.getCode(), customMessage != null ? customMessage : errorCode.getDefaultMessage(), errorCode.getHttpStatus());
        this.errorCode = errorCode;
    }

    public AuthErrorCode getErrorCode() {
        return errorCode;
    }
}
