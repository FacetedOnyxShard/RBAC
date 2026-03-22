package org.example;

import org.example.util.AuditLog;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AuditLogTest {

    @Test
    void log_shouldCreateEntry() {
        AuditLog log = new AuditLog();
        log.log("CREATE", "admin", "user", "details");
        List<AuditLog.AuditEntry> entries = log.getAll();
        assertEquals(1, entries.size());
        AuditLog.AuditEntry entry = entries.get(0);
        assertEquals("CREATE", entry.action());
        assertEquals("admin", entry.performer());
        assertEquals("user", entry.target());
    }

    @Test
    void getByPerformer_shouldFilter() {
        AuditLog log = new AuditLog();
        log.log("CREATE", "admin", "u1", null);
        log.log("DELETE", "admin", "u2", null);
        log.log("CREATE", "user1", "u3", null);
        List<AuditLog.AuditEntry> entries = log.getByPerformer("admin");
        assertEquals(2, entries.size());
    }

    @Test
    void getByAction_shouldFilter() {
        AuditLog log = new AuditLog();
        log.log("CREATE", "admin", "u1", null);
        log.log("DELETE", "admin", "u2", null);
        List<AuditLog.AuditEntry> entries = log.getByAction("CREATE");
        assertEquals(1, entries.size());
    }
}