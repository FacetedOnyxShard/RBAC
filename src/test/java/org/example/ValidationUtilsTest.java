package org.example;

import org.example.util.ValidationUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValidationUtilsTest {

    @Test
    void isValidUsername_shouldWork() {
        assertTrue(ValidationUtils.isValidUsername("john_doe"));
        assertFalse(ValidationUtils.isValidUsername("jo")); // too short
        assertFalse(ValidationUtils.isValidUsername("john$doe")); // invalid char
        assertFalse(ValidationUtils.isValidUsername("a".repeat(21))); // too long
    }

    @Test
    void isValidEmail_shouldWork() {
        assertTrue(ValidationUtils.isValidEmail("user@example.com"));
        assertFalse(ValidationUtils.isValidEmail("userexample.com"));
        assertFalse(ValidationUtils.isValidEmail("user@example"));
    }

    @Test
    void isValidDate_shouldWork() {
        assertTrue(ValidationUtils.isValidDate("2025-12-31 23:59:59"));
        assertFalse(ValidationUtils.isValidDate("2025-02-30 00:00:00"));
        assertFalse(ValidationUtils.isValidDate("2025/12/31 00:00:00"));
    }

    @Test
    void normalizeString_shouldTrimAndCollapseSpaces() {
        assertEquals("hello world", ValidationUtils.normalizeString("  hello   world  "));
        assertEquals("", ValidationUtils.normalizeString(null));
    }
}