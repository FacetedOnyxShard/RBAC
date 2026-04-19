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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class AuditLog {
    private final List<AuditEntry> entries = new ArrayList<>();;

    private final BlockingQueue<AuditEntry> logQueue = new LinkedBlockingQueue<>();
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Thread logProcessorThread;

    public AuditLog() {
        logProcessorThread = new Thread(() -> {
            while (running.get() || !logQueue.isEmpty()) {
                try {
                    AuditEntry entry = logQueue.take();
                    synchronized (entries) {
                        entries.add(entry);
                    }
                } catch (InterruptedException e) {
                     Thread.currentThread().interrupt();
                     break;
                }
            }

            List<AuditEntry> remaining = new ArrayList<>();
            logQueue.drainTo(remaining);
            synchronized (entries) {
                entries.addAll(remaining);
            }
        }, "AuditProcessor");

        logProcessorThread.setDaemon(true);
        logProcessorThread.start();
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

    public void shutdown() {
        running.set(false);
        logProcessorThread.interrupt();
        try {
            logProcessorThread.join(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void log(String action, String performer, String target, String details) {
        ValidationUtils.requireNonNullEmpty(action, "action");
        ValidationUtils.requireNonNullEmpty(performer, "performer");

        action = ValidationUtils.normalizeString(action, ValidationUtils.StringCase.UPPER_CASE);
        performer = ValidationUtils.normalizeString(performer);
        target = ValidationUtils.normalizeString(target);
        details = details == null ? "" : details;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String timestamp = LocalDateTime.now().format(formatter);

        AuditEntry entry = new AuditEntry(timestamp, action, performer, target, details);

        logQueue.offer(entry);
    }

    public List<AuditEntry> getAll() {
        synchronized (entries) {
            return new ArrayList<>(entries);
        }
    }

    public List<AuditEntry> getByPerformer(String performer) {
        synchronized (entries) {
            final String normalizedPerformer = ValidationUtils.normalizeString(performer);
            return entries.stream()
                    .filter((entry) -> entry.performer().equals(normalizedPerformer))
                    .toList();
        }
    }

    public List<AuditEntry> getByAction(String action) {
        synchronized (entries) {
            final String normalizedAction = ValidationUtils.normalizeString(action);
            return entries.stream()
                    .filter((entry) -> entry.action().equals(normalizedAction))
                    .toList();
        }
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

            synchronized (entries) {
                for (AuditEntry auditEntry : entries) {
                    logs.append(auditEntry.toString()).append("\n");
                }

                writer.print(logs);
            }

            System.out.printf("Logs successfully exported to %s\n", filename);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}