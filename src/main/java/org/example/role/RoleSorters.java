package org.example.role;

import java.util.Comparator;

public class RoleSorters {
    Comparator<Role> byName() {
        return Comparator.comparing(Role::getName,
                Comparator.nullsLast(String::compareTo));
    }

    Comparator<Role> byPermissionCount() {
        return Comparator.comparing(role -> role.getPermissions().size(),
                Comparator.nullsLast(Integer::compareTo));
    }
}
