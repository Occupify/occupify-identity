package com.occupify.identity.util;

import com.occupify.identity.exception.AuthErrorCode;
import com.occupify.identity.exception.AuthException;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

class PasswordUtilTest {

    @Test
    void shouldValidatePasswordLength() {
        assertTrue(PasswordUtil.isValid("password123"));
        assertTrue(PasswordUtil.isValid("12345678"));
        assertFalse(PasswordUtil.isValid("short"));
        assertFalse(PasswordUtil.isValid(null));
        assertFalse(PasswordUtil.isValid("   "));
    }

    @Test
    void shouldThrowWhenPasswordIsInvalid() {
        assertDoesNotThrow(() -> PasswordUtil.validate("password123"));
        AuthException ex = assertThrows(AuthException.class, () -> PasswordUtil.validate("short"));
        assertEquals(AuthErrorCode.AUTH_002, ex.getErrorCode());
    }

    @Test
    void shouldValidatePasswordChange() {
        assertDoesNotThrow(() -> PasswordUtil.validatePasswordChange("oldPassword123", "newPassword456"));

        AuthException exSame = assertThrows(AuthException.class,
                () -> PasswordUtil.validatePasswordChange("samePassword123", "samePassword123"));
        assertEquals(AuthErrorCode.AUTH_014, exSame.getErrorCode());

        AuthException exShort = assertThrows(AuthException.class,
                () -> PasswordUtil.validatePasswordChange("oldPassword123", "short"));
        assertEquals(AuthErrorCode.AUTH_002, exShort.getErrorCode());
    }

    @Test
    void shouldVerifyBcryptHashForPassword123() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode("password123");
        System.out.println("BCRYPT_HASH_PASSWORD123=" + hash);
        assertTrue(encoder.matches("password123", hash));
    }
}
