package org.example.util;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class AuditLog {

    private final List<AuditEntry> entries;

    public AuditLog() {
        entries = new ArrayList<>();
    }

    public record AuditEntry(
            String timestamp,
            String action,
            String performer,
            String target,
            String details
    ) {
        @Override
        public String toString() {
            return String.format("[ %s | %s ] %s -> %s | DETAILS: %s",
                    timestamp, action, performer,
                    target != null ? target : "-",
                    details != null ? details : "-");
        }
    }

    public void log(String action, String performer, String target, String details) {
        ValidationUtils.requireNonNullEmpty(action, "action");
        ValidationUtils.requireNonNullEmpty(performer, "performer");

        action = ValidationUtils.normalizeString(action, ValidationUtils.StringCase.UPPER_CASE);
        performer = ValidationUtils.normalizeString(performer);
        target = ValidationUtils.normalizeString(target);
        details = details == null ? "" : details;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String timestamp = LocalDateTime.now().format(formatter);

        AuditEntry entry = new AuditEntry(timestamp, action, performer, target, details);

        entries.add(entry);
    }

    public List<AuditEntry> getAll() {
        return new ArrayList<>(entries);
    }

    public List<AuditEntry> getByPerformer(String performer) {
        final String normalizedPerformer = ValidationUtils.normalizeString(performer);

        return entries.stream()
                .filter((entry) -> entry.performer().equals(normalizedPerformer))
                .toList();
    }

    public List<AuditEntry> getByAction(String action) {
        final String normalizedAction = ValidationUtils.normalizeString(action);

        return entries.stream()
                .filter((entry) -> entry.action().equals(normalizedAction))
                .toList();
    }

    public void printLog() {
        System.out.println("Записи логов:");
        for (AuditEntry entry: entries) {
            System.out.println(entry);
        }
    }

    public void saveToFile(String filename) {
        Path path = Paths.get(filename);
        if (path.getParent() != null) {
            try {
                Files.createDirectories(path.getParent());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            StringBuilder logs = new StringBuilder("Список логов:\n");
            for (AuditEntry auditEntry : entries) {
                logs.append(auditEntry.toString()).append("\n");
            }

            writer.print(logs);

            System.out.printf("Logs successfully exported to %s\n", filename);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}