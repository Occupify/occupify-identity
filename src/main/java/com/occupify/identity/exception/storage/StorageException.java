package com.occupify.identity.exception.storage;

import com.occupify.identity.exception.BaseException;
import lombok.Getter;

@Getter
public class StorageException extends BaseException {

    private final StorageErrorCode errorCode;

    public StorageException(StorageErrorCode errorCode) {
        super(errorCode.getCode(), errorCode.getMessage(), errorCode.getHttpStatus());
        this.errorCode = errorCode;
    }

    public StorageException(StorageErrorCode errorCode, String customMessage) {
        super(errorCode.getCode(), customMessage != null ? customMessage : errorCode.getMessage(), errorCode.getHttpStatus());
        this.errorCode = errorCode;
    }

    public StorageException(StorageErrorCode errorCode, String customMessage, Throwable cause) {
        super(errorCode.getCode(), customMessage != null ? customMessage : errorCode.getMessage(), errorCode.getHttpStatus());
        this.errorCode = errorCode;
        if (cause != null) {
            initCause(cause);
        }
    }
}
