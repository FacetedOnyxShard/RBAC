package org.example.util;

import java.util.List;

public class AuditLog {

    private List<AuditEntry> entries;

    public record AuditEntry(
            String timestamp,
            String action,
            String performer,
            String target,
            String details
    ) {}

    public void log(String action, String performer, String target, String details) {
        // TODO: Implement
    }

    public List<AuditEntry> getAll() {
        // TODO: Implement
        return null;
    }

    public List<AuditEntry> getByPerformer(String performer) {
        // TODO: Implement
        return null;
    }

    public List<AuditEntry> getByAction(String action) {
        // TODO: Implement
        return null;
    }

    public void printLog() {
        // TODO: Implement
    }

    public void saveToFile(String filename) {
        // TODO: Implement
    }
}