package org.example.role;

import org.example.assignment.AssignmentManager;
import org.example.core.Permission;
import org.example.core.Repository;

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
            throw new IllegalArgumentException("Input param must be not null object");
        }

        if (rolesByNameIdx.containsKey(item.getName())) {
            throw new IllegalArgumentException("This name already exists: " + item.getName());
        }

        rolesById.put(item.getId(), item);
        rolesByNameIdx.put(item.getName(), item);
    }

    @Override
    public boolean remove(Role item) {
        if (!assignmentManager.findByRole(item).isEmpty()) {
            return false;
        }

        if (!rolesById.remove(item.getId(), item)) {
            return false;
        }

        if (!rolesByNameIdx.remove(item.getName(), item)) {
            return false;
        }

        Set<String> usedNames = item.getUsedNames();
        return usedNames.remove(item.getName());
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
