package com.occupify.identity.util;

import com.occupify.identity.exception.AuthException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class EmailUtilTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "user@gmail.com",
            "admin@gmail.com",
            "candidate.test@occupify.vn",
            "user+alias@sub.domain.org"
    })
    void shouldValidateValidEmails(String email) {
        assertTrue(EmailUtil.isValid(email));
        assertDoesNotThrow(() -> EmailUtil.validate(email));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "   ",
            "plainaddress",
            "@missingusername.com",
            "missingdomain@.com",
            "missingat.com",
            "two@@at.com",
            "spaces in@domain.com"
    })
    void shouldRejectInvalidEmails(String email) {
        assertFalse(EmailUtil.isValid(email));
        assertThrows(AuthException.class, () -> EmailUtil.validate(email));
    }

    @Test
    void shouldNormalizeEmail() {
        assertEquals("user@gmail.com", EmailUtil.normalize("  User@Gmail.COM  "));
        assertNull(EmailUtil.normalize(null));
    }

    @Test
    void shouldMaskEmail() {
        assertEquals("u***r@gmail.com", EmailUtil.mask("user@gmail.com"));
        assertEquals("***", EmailUtil.mask("invalid-email"));
    }
}
