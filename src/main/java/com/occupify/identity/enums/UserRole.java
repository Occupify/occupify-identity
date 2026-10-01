package com.occupify.identity.enums;

public enum UserRole {
    USER,
    ADMIN;

    public static boolean isAdmin(String role) {
        return ADMIN.name().equalsIgnoreCase(role);
    }
}
