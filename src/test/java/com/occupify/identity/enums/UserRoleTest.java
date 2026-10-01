package com.occupify.identity.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserRoleTest {

    @Test
    void shouldRecognizeAdminRoleCaseInsensitively() {
        assertTrue(UserRole.isAdmin("ADMIN"));
        assertTrue(UserRole.isAdmin("admin"));
        assertTrue(UserRole.isAdmin("Admin"));
    }

    @Test
    void shouldRejectNonAdminRoles() {
        assertFalse(UserRole.isAdmin("USER"));
        assertFalse(UserRole.isAdmin("user"));
        assertFalse(UserRole.isAdmin("GUEST"));
        assertFalse(UserRole.isAdmin(null));
        assertFalse(UserRole.isAdmin(""));
    }

    @Test
    void shouldVerifyUserStatusAuthenticationEligibility() {
        assertTrue(UserStatus.ACTIVE.canAuthenticate());
        assertFalse(UserStatus.INACTIVE.canAuthenticate());
        assertFalse(UserStatus.BANNED.canAuthenticate());
    }
}
