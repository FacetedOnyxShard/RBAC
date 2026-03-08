package org.example.assignment;

import org.example.role.Role;
import org.example.user.User;

public interface RoleAssignment {
    String assignmentId();
    User user();
    Role role();
    AssignmentMetadata metadata();
    boolean isActive();
    String assignmentType();

    String summary(int n);
}
