package com.occupify.identity.enums;

public enum UserStatus {
    ACTIVE,
    INACTIVE,
    BANNED;

    public boolean canAuthenticate() {
        return this == ACTIVE;
    }
}
