package com.occupify.identity.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OtpTypeTest {

    @Test
    @DisplayName("Should parse OtpType case-insensitively and handle hyphens")
    void shouldParseCaseInsensitiveAndHyphen() {
        assertEquals(OtpType.REGISTER, OtpType.fromString("REGISTER"));
        assertEquals(OtpType.REGISTER, OtpType.fromString("register"));
        assertEquals(OtpType.REGISTER, OtpType.fromString("Register"));

        assertEquals(OtpType.FORGOT_PASSWORD, OtpType.fromString("FORGOT_PASSWORD"));
        assertEquals(OtpType.FORGOT_PASSWORD, OtpType.fromString("forgot_password"));
        assertEquals(OtpType.FORGOT_PASSWORD, OtpType.fromString("forgot-password"));

        assertNull(OtpType.fromString(null));
        assertNull(OtpType.fromString("   "));

        assertThrows(IllegalArgumentException.class, () -> OtpType.fromString("UNKNOWN"));
    }
}
