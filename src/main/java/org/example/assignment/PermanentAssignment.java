package org.example.assignment;

import org.example.role.Role;
import org.example.user.User;

public class PermanentAssignment extends AbstractRoleAssignment {
    private boolean revoked = false;

    public PermanentAssignment(User user, Role role, AssignmentMetadata metadata) {
        super(user, role, metadata);
    }

    @Override
    public boolean isActive() {
        return !revoked;
    }

    @Override
    public String assignmentType() {
        return "PERMANENT";
    }

    public void revoke() {
        revoked = true;
    }

    boolean isRevoked() {
        return revoked;
    }
}
