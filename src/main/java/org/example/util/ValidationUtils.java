package org.example.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

public class ValidationUtils {

    public static void isValidUsernameSymbols(String username) throws IllegalArgumentException {
        boolean isValidUsername = Pattern.matches("[A-Za-z0-9_]+", username);
        if (!isValidUsername) {
            throw new IllegalArgumentException("Username must contains only: Latin letters, underscore symbols");
        }
    }

    public static void isValidUsernameSize(String username) throws IllegalArgumentException {
        if (username.length() < 3 || username.length() > 20) {
            throw new IllegalArgumentException("Username length must be between 3 and 20");
        }
    }

    public static boolean isValidUsername(String username) {
        try {
            isValidUsernameSymbols(username);
        } catch (IllegalArgumentException e) {
            return false;
        }

        try {
            isValidUsernameSize(username);
        } catch (IllegalArgumentException e) {
            return false;
        }

        return true;
    }

    public static boolean isValidEmail(String email) {
        boolean isValidEmail = Pattern.matches("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}", email);
        if (!isValidEmail) {
            return false;
        }
        return true;
    }

    public static boolean isValidDate(String date) {
        if (date == null || date.length() != "2026-02-07".length()) {
            return false;
        }

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate.parse(date, formatter);
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    public static String normalizeString(String input) {
        if (input == null) return "";
        return input.trim().replaceAll("\\s+", " ");
    }

    public enum StringCase {
        LOWER_CASE,
        UPPER_CASE,
    }

    public static String normalizeString(String input, StringCase preferredCase) {
        if (preferredCase == StringCase.LOWER_CASE) input = input.toLowerCase();
        else input = input.toUpperCase();
        return normalizeString(input);
    }

    public static void requireNonNullEmpty(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Поле " + fieldName + " не может быть пустым");
        }
    }
}