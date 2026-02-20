package org.example;

import java.util.Comparator;

public class RoleSorters {
    Comparator<Role> byName() {
        return Comparator.comparing(role -> role.name,
                Comparator.nullsLast(String::compareTo));
    }

    Comparator<Role> byPermissionCount() {
        return Comparator.comparing(role -> role.getPermissions().size(),
                Comparator.nullsLast(Integer::compareTo));
    }
}
