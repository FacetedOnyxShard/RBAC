package org.example.util;

import org.example.assignment.AssignmentManager;
import org.example.assignment.RoleAssignment;
import org.example.core.Permission;
import org.example.role.Role;
import org.example.role.RoleManager;
import org.example.user.User;
import org.example.user.UserManager;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class ReportGenerator {
    public String generateUserReport(UserManager userManager, AssignmentManager assignmentManager) {
        StringBuilder sb = new StringBuilder();

        List<User> users = userManager.findAll();

        if (users.isEmpty()) {
            sb.append("No users found in the system\n");
            return sb.toString();
        }

        for (User user : users) {
            sb.append(String.format("%s: ", user.username()));

            List<RoleAssignment> activeAssignments = assignmentManager.findByUser(user).stream()
                    .filter(RoleAssignment::isActive)
                    .toList();

            if (activeAssignments.isEmpty()) {
                sb.append("no active roles");
            } else {
                String roles = activeAssignments.stream()
                        .map(assignment -> assignment.role().getName())
                        .collect(Collectors.joining(", "));
                sb.append(roles);
            }

            sb.append("\n");
        }

        return sb.toString();
    }

    public String generateRoleReport(RoleManager roleManager, AssignmentManager assignmentManager) {
        StringBuilder sb = new StringBuilder();

        List<Role> roles = roleManager.findAll();

        if (roles.isEmpty()) {
            sb.append("No roles found in the system\n");
            return sb.toString();
        }

        for (Role role : roles) {
            List<RoleAssignment> assignments = assignmentManager.findByRole(role);

            long userCount = assignments.stream()
                    .filter(RoleAssignment::isActive)
                    .map(RoleAssignment::user)
                    .distinct()
                    .count();

            sb.append(String.format("%s: %d\n", role.getName(), userCount));
        }

        return sb.toString();
    }

    public String generatePermissionMatrix(UserManager userManager, AssignmentManager assignmentManager) {
        StringBuilder sb = new StringBuilder();

        List<User> users = userManager.findAll();

        if (users.isEmpty()) {
            sb.append("No users found in the system\n");
            return sb.toString();
        }

        Set<String> allResources = new TreeSet<>();
        Map<User, Set<Permission>> userPermissions = new HashMap<>();

        for (User user : users) {
            Set<Permission> permissions = assignmentManager.getUserPermissions(user);
            userPermissions.put(user, permissions);
            permissions.stream()
                    .map(Permission::resource)
                    .forEach(allResources::add);
        }

        if (allResources.isEmpty()) {
            sb.append("No permissions found in the system\n");
            return sb.toString();
        }

        Map<User, Map<String, String>> matrix = new HashMap<>();

        for (User user : users) {
            Map<String, String> userResourcePermissions = new TreeMap<>();
            Set<Permission> permissions = userPermissions.getOrDefault(user, Collections.emptySet());

            for (String resource : allResources) {
                String permissionLetters = permissions.stream()
                        .filter(p -> p.resource().equals(resource))
                        .map(p -> String.valueOf(p.name().charAt(0))) // Берём первую букву
                        .sorted()
                        .collect(Collectors.joining(""));

                if (!permissionLetters.isEmpty()) {
                    userResourcePermissions.put(resource, permissionLetters);
                }
            }

            matrix.put(user, userResourcePermissions);
        }

        sb.append(String.format("%-20s", "User\\Resource"));
        for (String resource : allResources) {
            sb.append(String.format(" | %-10s", truncate(resource, 10)));
        }
        sb.append("\n");
        sb.append("-".repeat(20 + allResources.size() * 13)).append("\n");

        for (User user : users) {
            sb.append(String.format("%-20s", truncate(user.username(), 20)));

            Map<String, String> userResourcePermissions = matrix.get(user);

            for (String resource : allResources) {
                String perms = userResourcePermissions.getOrDefault(resource, "");

                if (perms.isEmpty()) {
                    sb.append(String.format(" | %-10s", "—"));
                } else {
                    sb.append(String.format(" | %-10s", perms));
                }
            }
            sb.append("\n");
        }

        sb.append("\nLegend (first letters):\n");

        Set<String> allFirstLetters = new TreeSet<>();
        for (User user : users) {
            Set<Permission> permissions = userPermissions.get(user);
            permissions.stream()
                    .map(p -> String.valueOf(p.name().charAt(0)))
                    .forEach(allFirstLetters::add);
        }

        if (!allFirstLetters.isEmpty()) {
            sb.append("  ");
            for (String letter : allFirstLetters) {
                String fullName = userPermissions.values().stream()
                        .flatMap(Set::stream)
                        .map(Permission::name)
                        .filter(name -> name.startsWith(letter))
                        .findFirst()
                        .orElse(letter);
                sb.append(String.format("%s=%s ", letter, fullName));
            }
            sb.append("\n");
        }

        sb.append("  — = no permissions\n");

        return sb.toString();
    }

    public void exportToFile(String report, String filepath) throws IOException {
        Path path = Paths.get(filepath);
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(filepath))) {
            writer.print(report);
        }

        System.out.printf("Report successfully exported to %s\n", filepath);
    }

    private String truncate(String str, int maxLength) {
        if (str == null) return "";
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength - 4) + "...";
    }

    public String generateUserReportParallel(UserManager userManager, AssignmentManager assignmentManager) {
        List<User> users = userManager.findAll();
        if (users.isEmpty()) {
            return "No users found in the system\n";
        }

        Map<String, String> usernameToRolesDescription = buildUsernameToRolesMapInParallel(users, assignmentManager);
        return formatUserRolesReport(usernameToRolesDescription);
    }

    private Map<String, String> buildUsernameToRolesMapInParallel(List<User> users, AssignmentManager assignmentManager) {
        return users.parallelStream()
                .collect(Collectors.toConcurrentMap(
                        User::username,
                        user -> describeActiveRolesForUser(user, assignmentManager)
                ));
    }

    private String describeActiveRolesForUser(User user, AssignmentManager assignmentManager) {
        List<RoleAssignment> activeAssignments = assignmentManager.findByUser(user).stream()
                .filter(RoleAssignment::isActive)
                .toList();

        if (activeAssignments.isEmpty()) {
            return "no active roles";
        }
        return activeAssignments.stream()
                .map(assignment -> assignment.role().getName())
                .collect(Collectors.joining(","));
    }

    private String formatUserRolesReport(Map<String, String> usernameToRolesDescription) {
        StringBuilder reportBuilder = new StringBuilder();
        usernameToRolesDescription.forEach((username, rolesDescription) ->
                reportBuilder.append(String.format("%s: %s\n", username, rolesDescription))
        );
        return reportBuilder.toString();
    }

    public String generatePermissionMatrixParallel(UserManager userManager, AssignmentManager assignmentManager) {
        List<User> users = userManager.findAll();
        if (users.isEmpty()) {
            return "No users found in the system\n";
        }

        Map<User, Set<Permission>> userToPermissions = buildUserToPermissionsMapInParallel(users, assignmentManager);
        Set<String> allResources = collectAllResourcesSorted(userToPermissions);
        if (allResources.isEmpty()) {
            return "No permissions found in the system\n";
        }

        Map<User, Map<String, String>> permissionMatrix = buildPermissionMatrixInParallel(users, userToPermissions, allResources);
        return formatPermissionMatrixReport(users, allResources, permissionMatrix, userToPermissions);
    }

    private Map<User, Set<Permission>> buildUserToPermissionsMapInParallel(List<User> users, AssignmentManager assignmentManager) {
        return users.parallelStream()
                .collect(Collectors.toConcurrentMap(
                        user -> user,
                        assignmentManager::getUserPermissions
                ));
    }

    private Set<String> collectAllResourcesSorted(Map<User, Set<Permission>> userToPermissions) {
        return userToPermissions.values().stream()
                .flatMap(Set::stream)
                .map(Permission::resource)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private Map<User, Map<String, String>> buildPermissionMatrixInParallel(
            List<User> users,
            Map<User, Set<Permission>> userToPermissions,
            Set<String> allResources) {
        return users.parallelStream()
                .collect(Collectors.toConcurrentMap(
                        user -> user,
                        user -> buildResourceToPermissionLettersMap(user, userToPermissions, allResources)
                ));
    }

    private Map<String, String> buildResourceToPermissionLettersMap(
            User user,
            Map<User, Set<Permission>> userToPermissions,
            Set<String> allResources) {
        Map<String, String> resourceToLetters = new TreeMap<>();
        Set<Permission> userPermissions = userToPermissions.getOrDefault(user, Collections.emptySet());
        for (String resource : allResources) {
            String letters = extractSortedPermissionLettersForResource(userPermissions, resource);
            if (!letters.isEmpty()) {
                resourceToLetters.put(resource, letters);
            }
        }
        return resourceToLetters;
    }

    private String extractSortedPermissionLettersForResource(Set<Permission> permissions, String resource) {
        return permissions.stream()
                .filter(permission -> permission.resource().equals(resource))
                .map(permission -> String.valueOf(permission.name().charAt(0)))
                .sorted()
                .collect(Collectors.joining());
    }

    private String formatPermissionMatrixReport(
            List<User> users,
            Set<String> allResources,
            Map<User, Map<String, String>> permissionMatrix,
            Map<User, Set<Permission>> userToPermissions) {
        int userColumnWidth = 20;
        int resourceColumnWidth = 10;
        StringBuilder reportBuilder = new StringBuilder();

        reportBuilder.append(String.format("%-" + userColumnWidth + "s", "User\\Resource"));
        for (String resource : allResources) {
            reportBuilder.append(String.format(" | %-" + resourceColumnWidth + "s", truncate(resource, resourceColumnWidth)));
        }
        reportBuilder.append("\n");

        int lineLength = userColumnWidth + allResources.size() * (resourceColumnWidth + 3);
        reportBuilder.append("-".repeat(lineLength)).append("\n");

        for (User user : users) {
            reportBuilder.append(String.format("%-" + userColumnWidth + "s", truncate(user.username(), userColumnWidth)));
            Map<String, String> resourceToLetters = permissionMatrix.get(user);
            for (String resource : allResources) {
                String letters = resourceToLetters != null ? resourceToLetters.getOrDefault(resource, "") : "";
                String cellValue = letters.isEmpty() ? "—" : letters;
                reportBuilder.append(String.format(" | %-" + resourceColumnWidth + "s", cellValue));
            }
            reportBuilder.append("\n");
        }

        reportBuilder.append("\nLegend (first letters):\n");
        Set<String> allFirstLetters = collectAllFirstLettersSorted(userToPermissions);
        if (!allFirstLetters.isEmpty()) {
            reportBuilder.append(" ");
            for (String letter : allFirstLetters) {
                String fullName = findFullPermissionNameByFirstLetter(userToPermissions, letter);
                reportBuilder.append(letter).append("=").append(fullName).append(" ");
            }
            reportBuilder.append("\n");
        }
        reportBuilder.append(" - = no permissions\n");

        return reportBuilder.toString();
    }

    private Set<String> collectAllFirstLettersSorted(Map<User, Set<Permission>> userToPermissions) {
        return userToPermissions.values().stream()
                .flatMap(Set::stream)
                .map(permission -> String.valueOf(permission.name().charAt(0)))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private String findFullPermissionNameByFirstLetter(Map<User, Set<Permission>> userToPermissions, String firstLetter) {
        return userToPermissions.values().stream()
                .flatMap(Set::stream)
                .map(Permission::name)
                .filter(name -> name.startsWith(firstLetter))
                .findFirst()
                .orElse(firstLetter);
    }
}