package com.occupify.identity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.springframework.lang.Nullable;

public enum OtpType {
    REGISTER,
    FORGOT_PASSWORD;

    @JsonCreator
    public static @Nullable OtpType fromString(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().replace('-', '_');
        for (OtpType type : OtpType.values()) {
            if (type.name().equalsIgnoreCase(normalized)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown OTP type: " + value + ". Allowed: REGISTER, FORGOT_PASSWORD");
    }
}
