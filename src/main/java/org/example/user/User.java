package org.example.user;

import org.example.util.ValidationUtils;

import java.util.regex.Pattern;

public record User(String username, String fullName, String email) {
    public static User validate(String username, String fullName, String email) {
        String[] fields = {username, fullName, email};

        for (String field : fields) {
            if (field == null || field.isBlank()) {
                throw new IllegalArgumentException("fields must not be null or empty");
            }
        }

        try {
            ValidationUtils.isValidUsernameSymbols(username);
            ValidationUtils.isValidUsernameSize(username);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(e);
        }

        if (!ValidationUtils.isValidEmail(email)) {
            throw new IllegalArgumentException("Email must be match normal pattern example@mail.com");
        }

        String[] nameParts = fullName.trim().split("\\s+");
        if (nameParts.length != 2) {
            throw new IllegalArgumentException("Full name must consist of first name and last name");
        }

        return new User(username, fullName, email);
    }

    public String format() {
        String[] nameParts = fullName.split("\\s+");
        return String.format("%s (%s %s) <%s>", username, nameParts[0], nameParts[1], email);
    }
}
