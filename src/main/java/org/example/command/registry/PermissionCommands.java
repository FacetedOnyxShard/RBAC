package org.example.command.registry;

import org.example.assignment.AssignmentFilters;
import org.example.assignment.RoleAssignment;
import org.example.command.CommandParser;
import org.example.core.Permission;
import org.example.user.User;
import org.example.util.ConsoleUtils;

import java.util.*;

public class PermissionCommands {
    public PermissionCommands(CommandParser parser) {
        parser.registerCommand("permissions-user",
                "все права конкретного пользователя",
                (scanner, system) -> {
                    Scanner inputScanner = new Scanner(System.in);

                    String username = ConsoleUtils.promptString(inputScanner, "Введите username:", true);

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

                    String username = ConsoleUtils.promptString(inputScanner,
                            "Введите username:", true);

                    String permissionName = ConsoleUtils.promptString(inputScanner,
                            "Введите наименование права:", true);

                    String resource = ConsoleUtils.promptString(inputScanner,
                            "Введите ресурс:", true);

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