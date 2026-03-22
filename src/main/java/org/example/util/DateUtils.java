package org.example.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class DateUtils {

    public static String getCurrentDate() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return LocalDate.now().format(formatter);
    }

    public static String getCurrentDateTime() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return LocalDateTime.now().format(formatter);
    }

    public static boolean isBefore(String date1, String date2) {
        LocalDate firstDate = LocalDate.parse(date1);
        LocalDate secondDate = LocalDate.parse(date2);

        return firstDate.isBefore(secondDate);
    }

    public static boolean isAfter(String date1, String date2) {
        LocalDate firstDate = LocalDate.parse(date1);
        LocalDate secondDate = LocalDate.parse(date2);

        return firstDate.isAfter(secondDate);
    }

    public static String addDays(String date, int days) {
        LocalDate localDate = LocalDate.parse(date);
        LocalDate newDate = localDate.plusDays(days);
        return newDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    public static String formatRelativeTime(String date) {
        LocalDate targetDate = LocalDate.parse(date);
        LocalDate now = LocalDate.now();

        long daysBetween = ChronoUnit.DAYS.between(now, targetDate);

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