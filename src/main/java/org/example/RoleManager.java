package org.example;

import java.util.*;

public class RoleManager implements Repository<Role> {
    private final Map<String, Role> rolesById; // ключ role.id
    private final Map<String, Role> rolesByNameIdx; // ключ role.name
    private AssignmentManager assignmentManager;

    public RoleManager() {
        rolesById = new HashMap<>();
        rolesByNameIdx = new HashMap<>();
    }

    public void setAssignmentManager(AssignmentManager assignmentManager) {
        this.assignmentManager = assignmentManager;
    }

    @Override
    public void add(Role item) {
        if (item == null) {
            throw new IllegalArgumentException();
        }

        if (rolesByNameIdx.containsKey(item.name)) {
            throw new IllegalArgumentException();
        }

        rolesById.put(item.id, item);
        rolesByNameIdx.put(item.name, item);
    }

    @Override
    public boolean remove(Role item) {
        if (!assignmentManager.findByRole(item).isEmpty()) {
            return false;
        }

        return rolesById.remove(item.id, item)
                && rolesByNameIdx.remove(item.name, item);
    }

    @Override
    public Optional<Role> findById(String id) {
        return Optional.ofNullable(rolesById.get(id));
    }

    @Override
    public List<Role> findAll() {
        return new ArrayList<>(rolesById.values());
    }

    @Override
    public int count() {
        return rolesById.size();
    }

    @Override
    public void clear() {
        for (Role role : rolesById.values()) {
            if (!this.remove(role))
                throw new IllegalStateException();
        }
    }

    public Optional<Role> findByName(String name) {
        return Optional.ofNullable(rolesByNameIdx.get(name));
    }

    public List<Role> findByFilter(RoleFilter filter) {
        return rolesById.values().stream()
                .filter(filter::test).toList();
    }

    public List<Role> findAll(RoleFilter filter, Comparator<Role> sorter) {
        return rolesById.values().stream()
                .filter(filter::test)
                .sorted(sorter).toList();
    }

    public boolean exists(String name) {
        return rolesByNameIdx.containsKey(name);
    }

    public void addPermissionToRole(String roleName, Permission permission) {
        findByName(roleName).ifPresent(role -> role.addPermission(permission));
    }

    public void removePermissionFromRole(String roleName, Permission permission) {
        findByName(roleName).ifPresent(role -> role.removePermission(permission));
    }

    public List<Role> findRolesWithPermission(String permissionName, String resource) {
        return rolesById.values().stream()
                .filter(role -> role.hasPermission(permissionName, resource))
                .toList();
    }
}
