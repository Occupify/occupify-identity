package com.occupify.identity.constant;

public final class RedisConstants {

    private RedisConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // Key patterns and prefixes
    public static final String KEY_PREFIX_REFRESH_TOKEN = "refresh_token:";
    public static final String KEY_PREFIX_USER_SESSIONS = "user_sessions:";
    public static final String KEY_PREFIX_REGISTER = "otp:register:";
    public static final String KEY_PREFIX_FORGOT_PASSWORD = "otp:forgot_password:";
    public static final String KEY_PREFIX_RESET_TOKEN = "password_reset_token:";

    // Token types
    public static final String TOKEN_TYPE_ACCESS = "ACCESS_TOKEN";
    public static final String TOKEN_TYPE_REFRESH = "REFRESH_TOKEN";
}
