package com.occupify.identity.constant;

import java.util.UUID;

public final class StorageConstants {

    private StorageConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // Storage folders
    public static final String FOLDER_AVATARS = "avatars";
    public static final String FOLDER_RESUMES = "resumes";
    public static final String FOLDER_PORTFOLIOS = "portfolios";
    public static final String FOLDER_CERTIFICATES = "certificates";
    public static final String FOLDER_EDUCATIONS = "educations";

    // Default Regions
    public static final String DEFAULT_OCI_REGION = "ap-singapore-1";
    public static final String DEFAULT_AWS_REGION = "us-east-1";

    // Public URL Format Templates
    public static final String OCI_PUBLIC_URL_FORMAT = "https://objectstorage.%s.oraclecloud.com/n/%s/b/%s/o/%s";
    public static final String AWS_PUBLIC_URL_FORMAT = "https://%s.s3.%s.amazonaws.com/%s";

    /**
     * Builds the directory path for user avatar: "avatars/{userId}"
     *
     * @param userId user unique identifier
     * @return directory path formatted as "avatars/{userId}"
     */
    public static String getAvatarPath(UUID userId) {
        return FOLDER_AVATARS + "/" + userId;
    }

    /**
     * Builds the directory path for user education proof document: "educations/{userId}"
     *
     * @param userId user unique identifier
     * @return directory path formatted as "educations/{userId}"
     */
    public static String getEducationDocumentPath(UUID userId) {
        return FOLDER_EDUCATIONS + "/" + userId;
    }

    /**
     * Builds the directory path for user qualification proof document: "certificates/{userId}"
     *
     * @param userId user unique identifier
     * @return directory path formatted as "certificates/{userId}"
     */
    public static String getQualificationDocumentPath(UUID userId) {
        return FOLDER_CERTIFICATES + "/" + userId;
    }
}
