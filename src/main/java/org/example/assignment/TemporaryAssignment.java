package org.example.assignment;

import org.example.role.Role;
import org.example.user.User;
import org.example.util.DateUtils;

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
        String now = DateUtils.getCurrentDateTime();
        return DateUtils.isBefore(now, expiresAt);
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
        if (isExpired()) {
            return "Expired";
        }
        return DateUtils.formatRelativeTime(expiresAt);
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
