package org.example.assignment;

import org.example.role.Role;
import org.example.user.User;
import org.example.user.UserFilter;

import java.util.function.Function;

public interface RoleAssignment {
    String assignmentId();
    User user();
    Role role();
    AssignmentMetadata metadata();
    boolean isActive();
    String assignmentType();

    String summary(int n);
    enum Type {
        PERMANENT("PERMANENT"),
        TEMPORARY("TEMPORARY");

        private final String StringView;

        Type(String StringView) {
            this.StringView = StringView;
        }

        @Override
        public String toString() {
            return StringView;
        }
    }
}
