package org.example.assignment;

import org.example.core.Permission;
import org.example.core.Repository;
import org.example.role.Role;
import org.example.role.RoleManager;
import org.example.user.User;
import org.example.user.UserManager;
import org.example.util.DateUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class AssignmentManager implements Repository<RoleAssignment> {
    private final Map<String, RoleAssignment> assignments = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private UserManager userManager;
    private RoleManager roleManager;

    public AssignmentManager() {
    }

    public void setUserManager(UserManager userManager) {
        this.userManager = userManager;
    }

    public void setRoleManager(RoleManager roleManager) {
        this.roleManager = roleManager;
    }

    @Override
    public void add(RoleAssignment item) {
        if (item == null) throw new IllegalArgumentException("Assignment cannot be null");

        lock.writeLock().lock();
        try {
            if (assignments.containsKey(item.assignmentId())) {
                throw new IllegalArgumentException("Duplicate assignment ID: " + item.assignmentId());
            }

            Optional<User> optionalUser = userManager.findByUsername(item.user().username());
            if (optionalUser.isEmpty()) {
                throw new IllegalArgumentException("User not found: " + item.user().username());
            }

            Optional<Role> optionalRole = roleManager.findById(item.role().getId());
            if (optionalRole.isEmpty()) {
                throw new IllegalArgumentException("Role not found: " + item.role().getId());
            }

            boolean alreadyActive = assignments.values().stream()
                    .filter(ass -> ass.user().equals(item.user()))
                    .filter(ass -> ass.role().equals(item.role()))
                    .anyMatch(RoleAssignment::isActive);
            if (alreadyActive) {
                throw new IllegalArgumentException("User already has an active assignment for role: " + item.role().getName());
            }

            assignments.put(item.assignmentId(), item);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean remove(RoleAssignment item) {
        lock.writeLock().lock();
        try {
            if (!assignments.containsKey(item.assignmentId())) {
                return false;
            }
            return assignments.remove(item.assignmentId(), item);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Optional<RoleAssignment> findById(String id) {
        lock.readLock().lock();
        try {
            return Optional.ofNullable(assignments.get(id));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<RoleAssignment> findAll() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(assignments.values());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public int count() {
        lock.readLock().lock();
        try {
            return assignments.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void clear() {
        lock.writeLock().lock();
        try {
            assignments.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<RoleAssignment> findByRole(Role role) {
        lock.readLock().lock();
        try {
            AssignmentFilter filter = AssignmentFilters.byRole(role);
            return assignments.values().stream()
                    .filter(filter::test)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<RoleAssignment> findByFilter(AssignmentFilter filter) {
        lock.readLock().lock();
        try {
            return assignments.values().stream()
                    .filter(filter::test)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<RoleAssignment> findByUser(User user) {
        if (user == null) {
            return Collections.emptyList();
        }
        lock.readLock().lock();
        try {
            AssignmentFilter assignmentFilterByUser = AssignmentFilters.byUser(user);
            return assignments.values().stream()
                    .filter(assignmentFilterByUser::test)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<RoleAssignment> findAll(AssignmentFilter filter, Comparator<RoleAssignment> sorter) {
        lock.readLock().lock();
        try {
            return assignments.values().stream()
                    .filter(filter::test)
                    .sorted(sorter)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<RoleAssignment> getActiveAssignments() {
        lock.readLock().lock();
        try {
            AssignmentFilter filter = AssignmentFilters.activeOnly();
            return assignments.values().stream()
                    .filter(filter::test)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<RoleAssignment> getExpiredAssignments() {
        lock.readLock().lock();
        try {
            return assignments.values().stream()
                    .filter(AssignmentFilters.inactiveOnly()::test)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean userHasRole(User user, Role role) {
        lock.readLock().lock();
        try {
            AssignmentFilter userFilter = AssignmentFilters.byUser(user);
            AssignmentFilter roleFilter = AssignmentFilters.byRole(role);
            return assignments.values().stream()
                    .anyMatch(userFilter.and(roleFilter)::test);
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean userHasPermission(User user, String permissionName, String resource) {
        lock.readLock().lock();
        try {
            AssignmentFilter userFilter = AssignmentFilters.byUser(user);
            AssignmentFilter hasPermissionFilter =
                    assignment -> assignment.role().hasPermission(permissionName, resource);
            return assignments.values().stream()
                    .anyMatch(userFilter.and(hasPermissionFilter)::test);
        } finally {
            lock.readLock().unlock();
        }
    }

    public Set<Permission> getUserPermissions(User user) {
        lock.readLock().lock();
        try {
            AssignmentFilter userFilter = AssignmentFilters.byUser(user);
            AssignmentFilter activeFilter = AssignmentFilters.activeOnly();
            return assignments.values().stream()
                    .filter(userFilter.and(activeFilter)::test)
                    .flatMap(assignment -> assignment.role().getPermissions().stream())
                    .collect(Collectors.toSet());
        } finally {
            lock.readLock().unlock();
        }
    }

    public void revokeAssignment(String assignmentId) {
        lock.writeLock().lock();
        try {
            RoleAssignment assignment = assignments.get(assignmentId);
            if (assignment == null) {
                throw new IllegalArgumentException("Assignment not found: " + assignmentId);
            }

            if (assignment instanceof PermanentAssignment permanentAssignment) {
                permanentAssignment.revoke();
            } else if (assignment instanceof TemporaryAssignment temporaryAssignment) {
                String currentDate = DateUtils.getCurrentDate();
                temporaryAssignment.setExpiresAt(currentDate);
            } else {
                throw new IllegalArgumentException("Unknown assignment type");
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void extendTemporaryAssignment(String assignmentId, String newExpirationDate) {
        lock.writeLock().lock();
        try {
            RoleAssignment assignment = assignments.get(assignmentId);
            if (assignment == null) {
                throw new IllegalArgumentException("Assignment not found: " + assignmentId);
            }
            if (assignment instanceof TemporaryAssignment temporaryAssignment) {
                temporaryAssignment.extend(newExpirationDate);
            } else {
                throw new IllegalArgumentException("Assignment is not temporary: " + assignmentId);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
}