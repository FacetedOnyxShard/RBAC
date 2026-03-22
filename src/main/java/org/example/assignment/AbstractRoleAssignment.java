package org.example.assignment;

import org.example.role.Role;
import org.example.user.User;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

public abstract class AbstractRoleAssignment implements RoleAssignment {
    String assignmentId;
    User user;
    Role role;
    AssignmentMetadata metadata;

    public AbstractRoleAssignment(User user, Role role, AssignmentMetadata metadata) {
        this.user = user;
        this.role = role;
        this.metadata = metadata;
        this.assignmentId = UUID.randomUUID().toString();
    }

    public String assignmentId() {
        return assignmentId;
    }

    public User user() {
        return user;
    }

    public Role role() {
        return role;
    }

    public AssignmentMetadata metadata() {
        return metadata;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass())  return false;
        AbstractRoleAssignment entity = (AbstractRoleAssignment) obj;
        return Objects.equals(assignmentId, entity.assignmentId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(assignmentId);
    }

    public abstract boolean isActive();
    public abstract String assignmentType();

    public String summary() {
        LocalDate dateTime = LocalDate.parse(metadata.assignedAt());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String formattedDate = dateTime.format(formatter);

        String summaryBaseTemplate = """
                [%s] %s assigned to %s by %s at %s
                Reason: %s
                Status: %s
                """;

        return String.format(summaryBaseTemplate, assignmentType(), role.getName(),
                user.username(), metadata.assignedBy(),
                formattedDate, metadata.reason(), isActive() ? "ACTIVE" : "INACTIVE"
                );
    }


    public String summary(int n) {
        LocalDate dateTime = LocalDate.parse(metadata.assignedAt());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String formattedDate = dateTime.format(formatter);

        String summaryBaseTemplate = """
                [%s] %s assigned to %s by %s at %s
                \t\tReason: %s
                \t\tStatus: %s
                """;

        return String.format(summaryBaseTemplate, assignmentType(), role.getName(),
                user.username(), metadata.assignedBy(),
                formattedDate, metadata.reason(), isActive() ? "ACTIVE" : "INACTIVE"
        );
    }
}
