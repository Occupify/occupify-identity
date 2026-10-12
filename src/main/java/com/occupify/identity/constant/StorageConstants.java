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

    /**
     * Builds the directory path for user avatar: "avatars/{userId}"
     *
     * @param userId user unique identifier
     * @return directory path formatted as "avatars/{userId}"
     */
    public static String getAvatarPath(UUID userId) {
        return FOLDER_AVATARS + "/" + userId;
    }
}
