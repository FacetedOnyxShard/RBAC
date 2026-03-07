package org.example.role;

import org.example.core.Permission;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class Role {
    private String id;
    private String name;
    private String description;
    private final Set<Permission> permissions;

    private static final Set<String> usedNames = new HashSet<>();

    public Role(String name) {
        this(name, null, null);
    }

    public Role(String name, String description) {
        this(name, description, null);
    }

    public Role(String name, String description,  Set<Permission> permissions) {
        if (name == null) {
            throw new IllegalArgumentException("Name must be not null");
        }

        String transformedName = name.trim();

        synchronized (usedNames) {
            if (usedNames.contains(transformedName)) {
                throw new IllegalArgumentException("This name already exists: " + transformedName);
            }
            usedNames.add(transformedName);
        }

        UUID uuid = UUID.randomUUID();
        this.id = "role_" + uuid;
        this.name = transformedName;
        this.description = description;
        this.permissions = permissions == null ? createDefaultSet() : permissions;
    }

    public Set<String> getUsedNames() {
        return usedNames;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    private static Set<Permission> createDefaultSet() {
        return new HashSet<>();
    }

    public void addPermission(Permission permission) {
        permissions.add(permission);
    }

    public void removePermission(Permission permission) {
        permissions.remove(permission);
    }

    public boolean hasPermission(Permission permission) {
        return permissions.contains(permission);
    }

    public boolean hasPermission(String permissionName, String resource) {
        boolean res = false;

        for (Permission p : permissions) {
            if (p.matches(permissionName, resource)) {
                res = true;
                break;
            }
        }

        return res;
    }

    public Set<Permission> getPermissions() {
        return Set.copyOf(permissions);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass())  return false;
        Role entity = (Role) obj;
        return Objects.equals(id, entity.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return String.format("Role{id='%s', name='%s'}", id, name);
    }

    public String format() {
        String header = """
                %s: %s [ID: %s]
                Description: %s
                """;

        String permissionsList = permissions.stream()
                .map(p -> " - " + p.format())
                .collect(Collectors.joining("\n"));


        return String.format(header + permissionsList, this.getClass().getSimpleName(), name, id, description);
    }
}
