package org.example.assignment;

import org.example.role.Role;
import org.example.user.User;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class TemporaryAssignment extends AbstractRoleAssignment {
    private String expiresAt;
    boolean autoRenew;

    public TemporaryAssignment(User user, Role role, AssignmentMetadata metadata, String expiresAt, boolean autoRenew) {
        super(user, role, metadata);
        this.expiresAt = expiresAt;
        this.autoRenew = autoRenew;
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(String expiresAt) {
        this.expiresAt = expiresAt;
    }

    @Override
    public boolean isActive() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = LocalDateTime.parse(expiresAt);
        return now.isBefore(deadline);
    }

    @Override
    public String assignmentType() {
        return Type.TEMPORARY.toString();
    }

    public void extend(String newExpirationDate) {
        this.expiresAt = newExpirationDate;
    }

    public boolean isExpired() {
        return !isActive();
    }

    public String getTimeRemaining() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = LocalDateTime.parse(expiresAt);

        if (isExpired()) {
            return "Expired";
        }

        long days = ChronoUnit.DAYS.between(now, deadline);
        long hours = ChronoUnit.HOURS.between(now, deadline) % 24;
        long minutes = ChronoUnit.MINUTES.between(now, deadline) % 60;

        return String.format("%d days, %d hours, %d minutes", days, hours, minutes);
    }

    @Override
    public String summary() {
        String baseSummary = super.summary();

        return baseSummary + "\n" + String.format("""
                Expires at: %s
                Remaining: %s
                Auto-renew: %s
                """, expiresAt, getTimeRemaining(),
                autoRenew ? "YES" : "NO");
    }
}
