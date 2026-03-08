package org.example.role;

import org.example.core.Permission;

public abstract class RoleFilters implements RoleFilter {
    public static RoleFilter byName(String name) {
        return role -> role.getName().equals(name);
    }

    public static RoleFilter byNameContains(String substring) {
        return role -> role.getName().contains(substring);
    }

    public static RoleFilter hasPermission(Permission permission) {
        return role -> role.hasPermission(permission);
    }

    public static RoleFilter hasPermission(String permissionName, String resource) {
        return role -> role.hasPermission(permissionName, resource);
    }

    public static RoleFilter hasAtLeastNPermissions(int n) {
        return role -> role.getPermissions().size() >= n;
    }
}
