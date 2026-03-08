package org.example.assignment;

import org.example.core.Permission;
import org.example.core.Repository;
import org.example.role.Role;
import org.example.role.RoleManager;
import org.example.user.User;
import org.example.user.UserManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class AssignmentManager implements Repository<RoleAssignment> {
    Map<String, RoleAssignment> assignments; // (ключ — assignmentId)
    private UserManager userManager;
    private RoleManager roleManager;

    public AssignmentManager() {
        this.assignments = new HashMap<>();
    }

    public void setUserManager(UserManager userManager) {
        this.userManager = userManager;
    }

    public void setRoleManager(RoleManager roleManager) {
        this.roleManager = roleManager;
    }

    @Override
    public void add(RoleAssignment item) {
        if (item == null) throw new IllegalArgumentException();
        if (assignments.containsKey(item.assignmentId())) {
            throw new IllegalArgumentException("Duplicate");
        }
        Optional<User> optionalUser =  userManager.findByUsername(item.user().username());
        if (optionalUser.isEmpty()) {
            throw new IllegalArgumentException();
        }
        Optional<Role> optionalRole = roleManager.findById(item.role().getId());
        if (optionalRole.isEmpty()) {
            throw new IllegalArgumentException();
        }


        assignments.put(item.assignmentId(), item);
    }

    @Override
    public boolean remove(RoleAssignment item) {
        if (!assignments.containsKey(item.assignmentId())) {
            return false;
        }
        return assignments.remove(item.assignmentId(), item);
    }

    @Override
    public Optional<RoleAssignment> findById(String id) {
        return Optional.ofNullable(assignments.get(id));
    }

    @Override
    public List<RoleAssignment> findAll() {
        return new ArrayList<>(assignments.values());
    }

    @Override
    public int count() {
        return assignments.size();
    }

    @Override
    public void clear() {
        assignments.clear();
    }

    public List<RoleAssignment> findByRole(Role role) {
        AssignmentFilter filter = AssignmentFilters.byRole(role);
        return assignments.values().stream()
                .filter(filter::test)
                .toList();
    }

    public List<RoleAssignment> findByFilter(AssignmentFilter filter) {
        return assignments.values().stream()
                .filter(filter::test)
                .toList();
    }

    public List<RoleAssignment> findAll(AssignmentFilter filter, Comparator<RoleAssignment> sorter){
        return assignments.values().stream()
                .filter(filter::test)
                .sorted(sorter).toList();
    }

    public List<RoleAssignment> getActiveAssignments() {
        AssignmentFilter filter = AssignmentFilters.activeOnly();
        return assignments.values().stream()
                .filter(filter::test).toList();
    }

    public List<RoleAssignment> getExpiredAssignments() {
        return assignments.values().stream()
                .filter(AssignmentFilters.inactiveOnly()::test)
                .toList();
    }

    public boolean userHasRole(User user, Role role) {
        AssignmentFilter userFilter = AssignmentFilters.byUser(user);
        AssignmentFilter roleFilter = AssignmentFilters.byRole(role);

        return !assignments.values().stream()
                .filter(userFilter.and(roleFilter)::test)
                .toList().isEmpty();
    }

    public boolean userHasPermission(User user, String permissionName, String resource) {
        AssignmentFilter userFilter =AssignmentFilters.byUser(user);
        AssignmentFilter hasPermissionFilter =
                assignment -> assignment.role().hasPermission(permissionName, resource);
        return !assignments.values().stream()
                .filter(userFilter.and(hasPermissionFilter)::test)
                .toList().isEmpty();
    }

    public Set<Permission> getUserPermissions(User user) {
        AssignmentFilter userFilter =  AssignmentFilters.byUser(user);
        AssignmentFilter activeFilter = AssignmentFilters.activeOnly();

        Optional<RoleAssignment> neededAssignment = assignments.values().stream()
                .filter(activeFilter.and(userFilter)::test)
                .findFirst();

        Set<Permission> result = Collections.emptySet();

        if (neededAssignment.isPresent()) {
            RoleAssignment assignment = neededAssignment.get();
            result = assignment.role().getPermissions();
        }

        return result;
    }

    public void revokeAssignment(String assignmentId) {
        RoleAssignment assignment =  assignments.get(assignmentId);

        if (assignment instanceof PermanentAssignment permanentAssignment) {
            permanentAssignment.revoke();
        } else if (assignment instanceof  TemporaryAssignment temporaryAssignment) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String currentDateTime = LocalDateTime.now().format(formatter);
            temporaryAssignment.setExpiresAt(currentDateTime);
        }
    }

    public void extendTemporaryAssignment(String assignmentId, String newExpirationDate) {
        RoleAssignment assignment =  assignments.get(assignmentId);

        if (assignment instanceof TemporaryAssignment temporaryAssignment) {
            temporaryAssignment.extend(newExpirationDate);
        }
    }
}
