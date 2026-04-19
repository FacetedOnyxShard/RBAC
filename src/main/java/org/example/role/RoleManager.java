package org.example.role;

import org.example.assignment.AssignmentManager;
import org.example.core.Permission;
import org.example.core.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class RoleManager implements Repository<Role> {
    private final Map<String, Role> rolesById; // ключ role.id
    private final Map<String, Role> rolesByNameIdx; // ключ role.name
    private volatile AssignmentManager assignmentManager;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public RoleManager() {
        rolesById = new ConcurrentHashMap<>();
        rolesByNameIdx = new ConcurrentHashMap<>();
    }

    public void setAssignmentManager(AssignmentManager assignmentManager) {
        this.assignmentManager = assignmentManager;
    }

    @Override
    public void add(Role item) {
        if (item == null) {
            throw new IllegalArgumentException("Input param must be not null object");
        }

        Role existingByName = rolesByNameIdx.putIfAbsent(item.getName(), item);
        if (existingByName != null) {
            throw new IllegalArgumentException("This name already exists: " + item.getName());
        }

        Role existingById = rolesById.putIfAbsent(item.getId(), item);
        if (existingById != null) {
            rolesByNameIdx.remove(item.getName());
            throw new IllegalArgumentException("Role with this ID already exists: " + item.getId());
        }
    }

    @Override
    public boolean remove(Role item) {
        if (item == null || assignmentManager == null) {
            return false;
        }

        if (!assignmentManager.findByRole(item).isEmpty()) {
            return false;
        }

        if (!rolesById.containsKey(item.getId()) ||
                !rolesByNameIdx.containsKey(item.getName())) {
            return false;
        }

        boolean removedByName = rolesByNameIdx.remove(item.getName(), item);
        boolean removedById = rolesById.remove(item.getId(), item);

        if (!removedByName) {
            rolesByNameIdx.put(item.getName(), item);
        }
        if (!removedById) {
            rolesById.put(item.getId(), item);
        }

        return removedByName && removedById;
    }

    public void updateRoleName(String oldName, String newName) {
        if (oldName.equals(newName)) {
            return;
        }

        lock.writeLock().lock();
        try {
            Role role = rolesByNameIdx.get(oldName);
            if (role == null) {
                throw new IllegalArgumentException("Role not found: " + oldName);
            }

            if (rolesByNameIdx.containsKey(newName)) {
                throw new IllegalArgumentException("Role with new name already exists: " + newName);
            }

            rolesByNameIdx.remove(oldName);
            role.setName(newName);
            rolesByNameIdx.put(newName, role);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Optional<Role> findById(String id) {
        lock.readLock().lock();
        try {
            return Optional.ofNullable(rolesById.get(id));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<Role> findAll() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(rolesById.values());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public int count() {
        lock.readLock().lock();
        try {
            return rolesById.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void clear() {
        lock.writeLock().lock();
        try {
            List<Role> snapshot = new ArrayList<>(rolesById.values());
            for (Role role : snapshot) {
                if (!this.remove(role))
                    throw new IllegalStateException();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Optional<Role> findByName(String name) {
        lock.readLock().lock();
        try {
            return Optional.ofNullable(rolesByNameIdx.get(name));
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<Role> findByFilter(RoleFilter filter) {
        lock.readLock().lock();
        try {
            return rolesById.values().stream()
                    .filter(filter::test).toList();
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<Role> findAll(RoleFilter filter, Comparator<Role> sorter) {
        lock.readLock().lock();
        try {
            return rolesById.values().stream()
                    .filter(filter::test)
                    .sorted(sorter).toList();
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean exists(String name) {

        lock.readLock().lock();
        try {
            return rolesByNameIdx.containsKey(name);
        } finally {
            lock.readLock().unlock();
        }

    }

    public void addPermissionToRole(String roleName, Permission permission) {
        findByName(roleName).ifPresent(role -> role.addPermission(permission));
    }

    public void removePermissionFromRole(String roleName, Permission permission) {
        findByName(roleName).ifPresent(role -> role.removePermission(permission));
    }

    public List<Role> findRolesWithPermission(String permissionName, String resource) {
        lock.readLock().lock();
        try {
            return rolesById.values().stream()
                    .filter(role -> role.hasPermission(permissionName, resource))
                    .toList();
        } finally {
            lock.readLock().unlock();
        }

    }

    public List<Role> findByFilterParallel(RoleFilter filter) {
        lock.readLock().lock();
        try {
            return rolesById.values()
                    .parallelStream()
                    .filter(filter::test)
                    .toList();
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<Role> findAllParallel(RoleFilter filter, Comparator<Role> sorter) {
        lock.readLock().lock();
        try {
            return rolesById.values()
                    .parallelStream()
                    .filter(filter::test)
                    .sorted(sorter)
                    .toList();
        } finally {
            lock.readLock().unlock();
        }
    }
}
