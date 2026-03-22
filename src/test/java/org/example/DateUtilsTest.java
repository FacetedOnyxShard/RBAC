package org.example;

import org.example.util.DateUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DateUtilsTest {

    @Test
    void isBefore_shouldWork() {
        assertTrue(DateUtils.isBefore("2023-01-01", "2023-01-02"));
        assertFalse(DateUtils.isBefore("2023-01-02", "2023-01-01"));
    }

    @Test
    void addDays_shouldWork() {
        assertEquals("2023-01-05", DateUtils.addDays("2023-01-01", 4));
        assertEquals("2022-12-31", DateUtils.addDays("2023-01-01", -1));
    }

    @Test
    void formatRelativeTime_shouldWork() {
        String today = DateUtils.getCurrentDate();
        assertEquals("today", DateUtils.formatRelativeTime(today));

        String tomorrow = DateUtils.addDays(today, 1);
        assertEquals("in 1 day", DateUtils.formatRelativeTime(tomorrow));

        String yesterday = DateUtils.addDays(today, -1);
        assertEquals("1 day ago", DateUtils.formatRelativeTime(yesterday));
    }
}