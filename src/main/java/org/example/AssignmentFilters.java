package org.example;


import java.time.LocalDateTime;

public abstract class AssignmentFilters implements AssignmentFilter {
    public static AssignmentFilter byUser(User user) {
        return assignment -> assignment.user().equals(user);
    }

    public static AssignmentFilter byUsername(String username) {
        return assignment -> assignment.user().username().equals(username);
    }

    public static AssignmentFilter byRole(Role role) {
        return assignment -> assignment.role().equals(role);
    }

    public static AssignmentFilter byRoleName(String roleName) {
        return assignment -> assignment.role().getName().equals(roleName);
    }

    public static AssignmentFilter activeOnly() {
        return RoleAssignment::isActive;
    }

    public static AssignmentFilter inactiveOnly() {
        return assignment -> !assignment.isActive();
    }

    public static AssignmentFilter byType(String type) {
        return assignment -> assignment.assignmentType().equals(type);
    }

    public static AssignmentFilter assignedBy(String username) {
        return assignment -> assignment.metadata().assignedBy().equals(username);
    }

    public static AssignmentFilter assignedAfter(String date)  {
        return assignment -> {
            LocalDateTime assignmentDate =  LocalDateTime.parse(assignment.metadata().assignedAt());
            LocalDateTime selectedDate = LocalDateTime.parse(date);
            return assignmentDate.isAfter(selectedDate);
        };
    }

    public static AssignmentFilter expiringBefore(String date) {
        return assignment -> {
            if (assignment instanceof TemporaryAssignment temporaryAssignment) {
                LocalDateTime expirationDate = LocalDateTime.parse(temporaryAssignment.expiresAt);
                LocalDateTime selectedDate = LocalDateTime.parse(date);
                return expirationDate.isBefore(selectedDate);
            }
            return false;
        };
    }
}
