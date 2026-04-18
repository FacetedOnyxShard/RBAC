package org.example.assignment;

import org.example.util.DateUtils;
import org.example.util.ValidationUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record AssignmentMetadata(String assignedBy, String assignedAt, String reason) {
    private static final String REASON_DEFAULT_VALUE = "";

    public AssignmentMetadata {
        reason = reason == null ? REASON_DEFAULT_VALUE : reason;
    }

    public AssignmentMetadata(String assignedBy, String assignedAt) {
        this(assignedBy, assignedAt, null);
    }

    public static AssignmentMetadata now(String assignedBy, String reason) {
        String assignedNow = DateUtils.getCurrentDateTime();
        return new AssignmentMetadata(assignedBy, assignedNow, reason);
    }

    public String format() {
        return String.format("Assignment by: %s, at: %s, reason: %s", assignedBy, assignedAt, reason);
    }
}
