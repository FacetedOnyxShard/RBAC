// UserTest.java
package org.example;

import org.example.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {
    @Test
    void createValidUser() {
        User user = User.validate("john_doe", "John Doe", "john@example.com");
        assertEquals("john_doe", user.username());
        assertEquals("John Doe", user.fullName());
        assertEquals("john@example.com", user.email());
    }

    @ParameterizedTest
    @MethodSource("provideInvalidUsers")
    void createInvalidUserThrowsException(String username, String fullName, String email, String expectedMessage) {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> User.validate(username, fullName, email)
        );
        assertTrue(exception.getMessage().contains(expectedMessage));
    }

    private static Stream<Arguments> provideInvalidUsers() {
        return Stream.of(
                Arguments.of(null, "John Doe", "john@example.com", "must not be null"),
                Arguments.of("john_doe", null, "john@example.com", "must not be null"),
                Arguments.of("john_doe", "John Doe", null, "must not be null"),
                Arguments.of("   ", "John Doe", "john@example.com", "must not be null"),
                Arguments.of("john$doe", "John Doe", "john@example.com", "latin letters, underscore"),
                Arguments.of("jo", "John Doe", "john@example.com", "between 3 and 20"),
                Arguments.of("j".repeat(21), "John Doe", "john@example.com", "between 3 and 20"),
                Arguments.of("john_doe", "John Doe", "invalid-email", "match normal pattern"),
                Arguments.of("john_doe", "John", "john@example.com", "first name and last name")
        );
    }

    @Test
    void testFormat() {
        User user = User.validate("john_doe", "John Doe", "john@example.com");
        assertEquals("john_doe (John Doe) <john@example.com>", user.format());
    }

    @Test
    void testEquality() {
        User user1 = User.validate("john_doe", "John Doe", "john@example.com");
        User user2 = User.validate("john_doe", "John Doe", "john@example.com");
        User user3 = User.validate("jane_doe", "Jane Doe", "jane@example.com");

        assertEquals(user1, user2);
        assertNotEquals(user1, user3);
        assertEquals(user1.hashCode(), user2.hashCode());
    }
}