package com.occupify.identity.enums;

public enum UserStatus {
    ACTIVE,
    INACTIVE,
    BANNED,
    DELETED;

    public boolean canAuthenticate() {
        return this == ACTIVE;
    }
}
