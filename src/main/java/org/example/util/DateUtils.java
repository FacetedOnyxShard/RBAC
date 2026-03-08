package org.example.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateUtils {

    public static String getCurrentDate() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return LocalDateTime.now().format(formatter);
    }

    public static String getCurrentDateTime() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return LocalDateTime.now().format(formatter);
    }

    public static boolean isBefore(String date1, String date2) {
        LocalDateTime firstDate = LocalDateTime.parse(date1);
        LocalDateTime secondDate = LocalDateTime.parse(date2);

        return firstDate.isBefore(secondDate);
    }

    public static boolean isAfter(String date1, String date2) {
        LocalDateTime firstDate = LocalDateTime.parse(date1);
        LocalDateTime secondDate = LocalDateTime.parse(date2);

        return firstDate.isAfter(secondDate);
    }

    public static String addDays(String date, int days) {
        // TODO: impl
        return null;
    }

    public static String formatRelativeTime(String date) {
        // TODO: Implement
        return null;
    }
}