package org.example;

import org.example.core.Permission;
import org.example.role.Role;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RoleTest {

    @Test
    void createRole_shouldGenerateId() {
        Role role = new Role("Admin");
        assertNotNull(role.getId());
        assertTrue(role.getId().startsWith("role_"));
        assertEquals("Admin", role.getName());
    }

    @Test
    void addRemovePermission_shouldWork() {
        Role role = new Role("Admin");
        Permission p = new Permission("READ", "users", "read users");
        role.addPermission(p);
        assertTrue(role.hasPermission(p));
        assertTrue(role.hasPermission("READ", "users"));

        role.removePermission(p);
        assertFalse(role.hasPermission(p));
    }

    @Test
    void getPermissions_returnsUnmodifiableSet() {
        Role role = new Role("Admin");
        Permission p = new Permission("READ", "users", "read users");
        role.addPermission(p);
        Set<Permission> perms = role.getPermissions();
        assertThrows(UnsupportedOperationException.class,
                () -> perms.add(new Permission("WRITE", "users", "write users")));
    }

    @Test
    void equalsHashCode_usesId() {
        Role role1 = new Role("Admin");
        Role role2 = new Role("Admin");
        role2.setId(role1.getId());  // для теста приравняем id
        assertEquals(role1, role2);
        assertEquals(role1.hashCode(), role2.hashCode());
    }
}