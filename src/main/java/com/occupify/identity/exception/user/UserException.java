package com.occupify.identity.exception.user;

import com.occupify.identity.exception.BaseException;
import lombok.Getter;

@Getter
public class UserException extends BaseException {

    private final UserErrorCode errorCode;

    public UserException(UserErrorCode errorCode) {
        super(errorCode.getCode(), errorCode.getMessage(), errorCode.getHttpStatus());
        this.errorCode = errorCode;
    }

    public UserException(UserErrorCode errorCode, String customMessage) {
        super(errorCode.getCode(), customMessage != null ? customMessage : errorCode.getMessage(), errorCode.getHttpStatus());
        this.errorCode = errorCode;
    }
}
