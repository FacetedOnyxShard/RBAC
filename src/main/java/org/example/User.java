package org.example;

import java.util.regex.Pattern;

public record User(String username, String fullName, String email) {
    public static User validate(String username, String fullName, String email) {
        String[] fields = {username, fullName, email};

        for (String field : fields) {
            if (field == null || field.isBlank()) {
                throw new IllegalArgumentException("fields must not be null or empty");
            }
        }

        boolean isValidUsername = Pattern.matches("[A-Za-z0-9_]+", username);
        if (!isValidUsername) {
            throw new IllegalArgumentException("Username must contains only: latin letters, underscore symbols");
        }

        if (username.length() < 3 || username.length() > 20) {
            throw new IllegalArgumentException("Username length must be between 3 and 20");
        }

        boolean isValidEmail = Pattern.matches("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}", email);
        if (!isValidEmail) {
            throw new IllegalArgumentException("Email must be match normal pattern example@mail.com");
        }

        String[] nameParts = fullName.trim().split("\\s+");
        if (nameParts.length != 2) {
            throw new IllegalArgumentException("Full name must consist of first name and last name");
        }

        return new User(username, fullName, email);
    }

    String format() {
        String[] nameParts = fullName.split("\\s+");
        return String.format("%s (%s %s) <%s>", username, nameParts[0], nameParts[1], email);
    }
}
