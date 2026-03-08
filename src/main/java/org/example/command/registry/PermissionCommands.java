package org.example.command.registry;

import org.example.assignment.AssignmentFilters;
import org.example.assignment.RoleAssignment;
import org.example.command.CommandParser;
import org.example.core.Permission;
import org.example.user.User;
import org.example.util.InputUtils;

import java.util.*;

public class PermissionCommands {
    public PermissionCommands(CommandParser parser) {
        parser.registerCommand("permissions-user",
                "все права конкретного пользователя",
                (scanner, system) -> {
                    Scanner inputScanner = new Scanner(System.in);

                    String username = InputUtils.readLine(inputScanner, "Введите username:");

                    Optional<User> optionalUser = system.getUserManager().findByUsername(username);
                    if (optionalUser.isEmpty()) {
                        System.out.println("Пользователь с ником " + username + " не найден");
                        return;
                    }
                    User user = optionalUser.get();

                    List<RoleAssignment> assignments = system.getAssignmentManager()
                            .findByFilter(AssignmentFilters.byUser(user));

                    if (assignments.isEmpty()) {
                        System.out.println("У пользователя " + username + " нет ролей");
                        return;
                    }

                    System.out.println("Права пользователя " + username + ":");

                    Map<String, Set<String>> resourceToPermissions = new HashMap<>();

                    for (RoleAssignment a : assignments) {
                        for (Permission p : a.role().getPermissions()) {
                            String resource = p.resource();
                            resourceToPermissions
                                    .computeIfAbsent(resource, k -> new HashSet<>())
                                    .add(p.name());
                        }
                    }

                    for (Map.Entry<String, Set<String>> entry : resourceToPermissions.entrySet()) {
                        System.out.println("  Ресурс: " + entry.getKey());
                        for (String perm : entry.getValue()) {
                            System.out.println("    - " + perm);
                        }
                    }
                });

        parser.registerCommand("permissions-check",
                "проверить, есть ли у пользователя конкретное право",
                (scanner, system) -> {

                    Scanner inputScanner = new Scanner(System.in);

                    String username = InputUtils.readLine(inputScanner,
                            "Введите username:");

                    String permissionName = InputUtils.readLine(inputScanner,
                            "Введите наименование права:");

                    String resource = InputUtils.readLine(inputScanner,
                            "Введите ресурс:");

                    Optional<User> optionalUser = system.getUserManager().findByUsername(username);
                    if (optionalUser.isEmpty()) {
                        System.out.println("Пользователь с ником " + username + " не найден");
                        return;
                    }
                    User user = optionalUser.get();

                    if (system.getAssignmentManager().userHasPermission(user, permissionName, resource)) {
                        System.out.println("YES. ");
                        for (RoleAssignment a : system.getAssignmentManager().findByFilter(AssignmentFilters.byUser(user))) {
                            if (a.role().hasPermission(permissionName, resource)) {
                                System.out.println("Из роли: " + a.role().getName());
                                break;
                            }
                        }
                    } else {
                        System.out.println("NO");
                    }
                });
    }
}