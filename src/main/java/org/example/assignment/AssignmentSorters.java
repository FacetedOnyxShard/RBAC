package org.example.assignment;

import java.time.LocalDateTime;
import java.util.Comparator;

public class AssignmentSorters {
    Comparator<RoleAssignment> byUsername() {
        return Comparator.comparing(roleAssignment -> roleAssignment.user().username(),
                Comparator.nullsLast(String::compareTo));
    }

    Comparator<RoleAssignment> byRoleName() {
        return Comparator.comparing(roleAssignment -> roleAssignment.role().getName(),
                Comparator.nullsLast(String::compareTo));
    }

    Comparator<RoleAssignment> byAssignmentDate() {
        return Comparator.comparing(
                roleAssignment -> LocalDateTime.parse(roleAssignment.metadata().assignedAt()),
                Comparator.nullsLast(LocalDateTime::compareTo)
        );
    }
}
