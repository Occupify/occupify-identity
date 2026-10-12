package com.occupify.identity.exception.storage;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum StorageErrorCode {

    FILE_EMPTY("STORAGE_001", "Uploaded file is empty or missing", HttpStatus.BAD_REQUEST),
    INVALID_FILE_TYPE("STORAGE_002", "Only JPEG, PNG, WEBP, and GIF images are allowed for avatars", HttpStatus.BAD_REQUEST),
    FILE_TOO_LARGE("STORAGE_003", "Uploaded file exceeds maximum allowed limit (5MB)", HttpStatus.BAD_REQUEST),
    UPLOAD_FAILED("STORAGE_004", "Failed to upload file to object storage", HttpStatus.INTERNAL_SERVER_ERROR),
    DELETE_FAILED("STORAGE_005", "Failed to delete file from object storage", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
