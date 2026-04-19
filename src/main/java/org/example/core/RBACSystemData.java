package org.example.core;

import org.example.assignment.AbstractRoleAssignment;
import org.example.role.Role;
import org.example.user.User;

import java.util.List;

public record RBACSystemData(
        List<User> users,
        List<Role> roles,
        List<AbstractRoleAssignment> assignments) {
}