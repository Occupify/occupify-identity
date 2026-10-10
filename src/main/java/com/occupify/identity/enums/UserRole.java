package com.occupify.identity.enums;

public enum UserRole {
    EMPLOYEE,
    EMPLOYER,
    ADMIN,
    @Deprecated
    USER;

    public static boolean isAdmin(String role) {
        return ADMIN.name().equalsIgnoreCase(role);
    }
}
