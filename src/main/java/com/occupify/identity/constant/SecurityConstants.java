package com.occupify.identity.constant;

public final class SecurityConstants {

    private SecurityConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_EMAIL = "X-User-Email";
    public static final String HEADER_USER_ROLE = "X-User-Role";
    public static final String HEADER_REFRESH_TOKEN = "X-Refresh-Token";
    public static final String BEARER_PREFIX = "Bearer ";
}
