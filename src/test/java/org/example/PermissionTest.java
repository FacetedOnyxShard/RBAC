package org.example;

import org.example.core.Permission;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PermissionTest {

    @Test
    void validPermission_shouldNormalize() {
        Permission p = new Permission(" read ", " USERS ", "  view users  ");
        assertEquals("READ", p.name());
        assertEquals("users", p.resource());
        assertEquals("view users", p.description());
    }

    @Test
    void nameContainsSpaces_shouldThrow() {
        assertThrows(IllegalArgumentException.class,
                () -> new Permission("READ WRITE", "users", "description"));
    }

    @Test
    void descriptionEmpty_shouldThrow() {
        assertThrows(IllegalArgumentException.class,
                () -> new Permission("READ", "users", "   "));
    }

    @Test
    void matches_shouldWork() {
        Permission p = new Permission("READ", "users", "read users");
        assertTrue(p.matches("READ", "users"));
        assertTrue(p.matches("REA", "ser"));   // contains
        assertFalse(p.matches("WRITE", "users"));
    }
}