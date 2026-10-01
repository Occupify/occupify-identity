package com.occupify.identity.exception;

import org.springframework.http.HttpStatus;

public class AuthException extends RuntimeException {

    private final AuthErrorCode errorCode;
    private final HttpStatus status;

    public AuthException(AuthErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.status = errorCode.getHttpStatus();
    }

    public AuthException(AuthErrorCode errorCode, String customMessage) {
        super(customMessage != null ? customMessage : errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.status = errorCode.getHttpStatus();
    }

    public AuthErrorCode getErrorCode() {
        return errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
