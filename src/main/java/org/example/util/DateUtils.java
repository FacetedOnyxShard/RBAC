package org.example.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class DateUtils {
    public static final DateTimeFormatter defaultFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static String getCurrentDate() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return LocalDate.now().format(formatter);
    }

    public static String getCurrentDateTime() {
        return LocalDateTime.now().format(defaultFormatter);
    }

    public static boolean isBefore(String date1, String date2) {
        DateTimeFormatter formatter = defaultFormatter;
        LocalDateTime firstDate = LocalDateTime.parse(date1, formatter);
        LocalDateTime secondDate = LocalDateTime.parse(date2, formatter);
        return firstDate.isBefore(secondDate);
    }

    public static boolean isAfter(String date1, String date2) {
        DateTimeFormatter formatter = defaultFormatter;
        LocalDateTime firstDate = LocalDateTime.parse(date1, formatter);
        LocalDateTime secondDate = LocalDateTime.parse(date2, formatter);
        return firstDate.isAfter(secondDate);
    }

    public static String addDays(String date, int days) {
        LocalDateTime localDate = LocalDateTime.parse(date, defaultFormatter);
        LocalDateTime newDate = localDate.plusDays(days);
        return newDate.format(defaultFormatter);
    }

    public static String formatRelativeTime(String date) {
        LocalDateTime targetDateTime = LocalDateTime.parse(date, defaultFormatter);

        LocalDate targetDate = targetDateTime.toLocalDate();
        LocalDate today = LocalDate.now();

        long daysBetween = ChronoUnit.DAYS.between(today, targetDate);

        if (daysBetween == 0) {
            return "today";
        } else if (daysBetween > 0) {
            return String.format("in %d day%s", daysBetween, daysBetween == 1 ? "" : "s");
        } else {
            long daysAgo = -daysBetween;
            return String.format("%d day%s ago", daysAgo, daysAgo == 1 ? "" : "s");
        }
    }
}