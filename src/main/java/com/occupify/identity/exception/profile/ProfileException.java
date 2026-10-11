package com.occupify.identity.exception.profile;

import com.occupify.identity.exception.BaseException;
import lombok.Getter;

@Getter
public class ProfileException extends BaseException {

    private final ProfileErrorCode errorCode;

    public ProfileException(ProfileErrorCode errorCode) {
        super(errorCode.getCode(), errorCode.getMessage(), errorCode.getHttpStatus());
        this.errorCode = errorCode;
    }

    public ProfileException(ProfileErrorCode errorCode, String customMessage) {
        super(errorCode.getCode(), customMessage != null ? customMessage : errorCode.getMessage(), errorCode.getHttpStatus());
        this.errorCode = errorCode;
    }
}
