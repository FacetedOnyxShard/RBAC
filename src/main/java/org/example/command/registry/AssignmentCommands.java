package org.example.command.registry;

import org.example.assignment.*;
import org.example.command.CommandParser;
import org.example.role.Role;
import org.example.user.User;
import org.example.util.ConsoleUtils;
import org.example.util.DateUtils;
import org.example.util.FormatUtils;
import org.example.util.ValidationUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class AssignmentCommands {
    public AssignmentCommands(CommandParser parser) {
        parser.registerCommand("assign-role",
                "назначить роль пользователю",
                (scanner, system) -> {
                    Scanner inputScanner = new Scanner(System.in);

                    parser.executeCommand("user-list", new Scanner(""), system);

                    String username;
                    username = ConsoleUtils.promptString(inputScanner,
                            "Введите username пользователя, которому хотите назначить роль:",
                            true);

                    parser.executeCommand("role-list", inputScanner, system);

                    int selectedRoleNumber = 0;
                    while (true) {
                        try {
                            selectedRoleNumber = ConsoleUtils.promptInt(inputScanner,
                                    "Введите номер желаемой роли:", 1, system.getRoleManager().count());
                            break;
                        } catch (Exception e) {
                            System.out.println("Выбранный номер должен быть в диапазоне от " + 1 + " до " +  system.getRoleManager().count());
                        }
                    }
                    final int selectedRoleIdx = selectedRoleNumber - 1;

                    int selectedAssignmentType = 0;
                    while (true) {
                        try {
                            selectedAssignmentType = ConsoleUtils.promptInt(inputScanner,
                                    "Выберите тип назначения (0 - постоянное, 1 - временное):", 0, 1);
                            break;
                        } catch (Exception e) {
                            System.out.println("Выбранный номер должен быть в диапазоне от " + 0 + " до " +  1);
                        }
                    }

                    String expirationDate = null;
                    if (selectedAssignmentType == 1) {
                        expirationDate = ConsoleUtils.promptString(inputScanner,
                                "Введите дату истечения (формат yyyy-MM-dd):", true);

                        if (!ValidationUtils.isValidDate(expirationDate)) {
                            throw new RuntimeException("Incorrect date format");
                        }
                    }

                    String reason = ConsoleUtils.promptString(inputScanner,
                            "Укажите причину назначения:", true);

                    AbstractRoleAssignment newAssignment;
                    AssignmentMetadata metadata = AssignmentMetadata.now(system.getCurrentUser(), reason);
                    List<Role> roleList = system.getRoleManager().findAll();

                    Optional<User> optionalUser = system.getUserManager().findByUsername(username);
                    if (optionalUser.isEmpty()) {
                        System.out.println("Ошибка: не удалось найти пользователя с username = " + username);
                        return;
                    }
                    User user = optionalUser.get();
                    Role selectedRole = roleList.get(selectedRoleIdx);

                    if (selectedAssignmentType == 1) {
                        newAssignment = new TemporaryAssignment(user, selectedRole, metadata,
                                expirationDate, false);
                    } else {
                        newAssignment = new PermanentAssignment(user, selectedRole, metadata);
                    }
                    system.getAssignmentManager().add(newAssignment);

                    System.out.printf("Роль %s успешно назначена пользователю %s.\n", selectedRole.getName(), username);

                    String details = String.format("role %s successfully assigned %s -> %s",
                            selectedRole.getName(), system.getCurrentUser(), user.username());
                    system.getAuditLog().log("ASSIGN_ROLE", system.getCurrentUser(),
                            user.username(), details);
                });

        parser.registerCommand("revoke-role",
                "отозвать роль у пользователя",
                (scanner, system) -> {
                    Scanner inputScanner = new Scanner(System.in);

                    parser.executeCommand("assignment-list", new Scanner(""), system);

                    String username = ConsoleUtils.promptString(inputScanner,
                            "Введите username пользователя у которого хотите отзвать роль:", true);

                    List<RoleAssignment> roleAssignmentListByUsername =
                            system.getAssignmentManager().findByFilter(AssignmentFilters.byUsername(username));

                    if (roleAssignmentListByUsername.isEmpty()) {
                        System.out.println("У этого пользователя нет назначений");
                        return;
                    }

                    System.out.println("Назначения пользователя " + username + ":");
                    int n = 1;
                    for (RoleAssignment roleAssignment : roleAssignmentListByUsername) {
                        System.out.printf("\t%d. %s\n", n, roleAssignment.metadata().format());
                        ++n;
                    }

                    int selectedAssignmentNumber;
                    try {
                        selectedAssignmentNumber = ConsoleUtils.promptInt(inputScanner,
                                "Введите номер назначения, которое хотите отозвать:",
                                1, roleAssignmentListByUsername.size());
                    } catch (RuntimeException e) {
                        throw new RuntimeException(e);
                    }
                    int selectedAssignmentIdx = selectedAssignmentNumber - 1;

                    RoleAssignment selectedAssignment = roleAssignmentListByUsername.get(selectedAssignmentIdx);

                    system.getAssignmentManager().revokeAssignment(selectedAssignment.assignmentId());

                    System.out.println("Роль успешно отозвана.");

                    system.getAuditLog().log("REVOKE_ROLE", system.getCurrentUser(),
                            username, null);
                });

        parser.registerCommand("assignment-list",
                "список всех назначений",
                (scanner, system) -> {
                    List<RoleAssignment> roleAssignmentList = system.getAssignmentManager().findAll();
                    String[] headers = {"username", "role", "type", "status", "assigned at", "assignment ID"};
                    List<String[]> rows = new ArrayList<>();

                    for (RoleAssignment roleAssignment : roleAssignmentList) {
                        rows.add(new String[]{
                                roleAssignment.user().username(),
                                roleAssignment.role().toString(),
                                roleAssignment.assignmentType(),
                                roleAssignment.isActive() ? "Active" : "Inactive",
                                roleAssignment.metadata().assignedAt(),
                                roleAssignment.assignmentId()
                        });
                    }

                    String table = FormatUtils.formatTable(headers, rows);
                    System.out.println(table);
                });

        parser.registerCommand("assignment-list-user",
                "назначения конкретного пользователя",
                (scanner, system) -> {
                    Scanner inputScanner = new Scanner(System.in);

                    parser.executeCommand("user-list", scanner, system);

                    String username = ConsoleUtils.promptString(inputScanner,
                            "Введите username:", true);

                    List<RoleAssignment> roleAssignmentList =
                            system.getAssignmentManager().findByFilter(AssignmentFilters.byUsername(username));
                    System.out.println("Назначения пользователя " + username + ":");
                    int n = 1;
                    for (RoleAssignment roleAssignment : roleAssignmentList) {
                        System.out.printf("\t%d. %s\n", n, roleAssignment.metadata().format());
                        ++n;
                    }
                });

        parser.registerCommand("assignment-list-role",
                "список пользователей с конкретной ролью",
                (scanner, system) -> {
                    Scanner inputScanner = new Scanner(System.in);

                    String roleName = ConsoleUtils.promptString(inputScanner,
                            "Введите название роли:", true);

                    List<RoleAssignment> roleAssignmentList =
                            system.getAssignmentManager().findByFilter(AssignmentFilters.byRoleName(roleName));
                    int n = 1;
                    for (RoleAssignment roleAssignment : roleAssignmentList) {
                        System.out.printf("\t%d. %s\n", n, roleAssignment.user().format());
                        ++n;
                    }
                });

        parser.registerCommand("assignment-active",
                "только активные назначения",
                (scanner, system) -> {
                    List<RoleAssignment> roleAssignmentList =
                            system.getAssignmentManager().findByFilter(AssignmentFilters.activeOnly());
                    int n = 1;
                    for (RoleAssignment roleAssignment : roleAssignmentList) {
                        System.out.printf("\t%d. User: %s; Role: %s\n", n, roleAssignment.user().username(), roleAssignment.role().toString());
                        ++n;
                    }
                });

        parser.registerCommand("assignment-expired",
                "истёкшие временные назначения",
                (scanner, system) -> {
                    String now = DateUtils.getCurrentDate();
                    List<RoleAssignment> roleAssignmentList =
                            system.getAssignmentManager().findByFilter(AssignmentFilters.expiringBefore(now));

                    if (roleAssignmentList.isEmpty()) {
                        System.out.println("Нет ни одного истекшего временного назначения");
                        return;
                    }

                    System.out.println("Список истекших временных назначений");
                    int n = 1;
                    for (RoleAssignment roleAssignment : roleAssignmentList) {
                        System.out.printf("\t%d. User: %s; Role: %s\n", n, roleAssignment.user().username(), roleAssignment.role().toString());
                        ++n;
                    }
                });

        parser.registerCommand("assignment-extend",
                "продлить временное назначение",
                (scanner, system) -> {
                    Scanner inputScanner = new Scanner(System.in);

                    parser.executeCommand("assignment-list", inputScanner, system);

                    String assignmentID = ConsoleUtils.promptString(inputScanner,
                            "Введите assignment id:", true);

                    String newExpirationDate = ConsoleUtils.promptString(inputScanner,
                            "Введите новый deadline (yyyy-MM-dd):", true);

                    if (!ValidationUtils.isValidDate(newExpirationDate)) {
                        throw new RuntimeException("Incorrect date format");
                    }

                    system.getAssignmentManager().extendTemporaryAssignment(assignmentID, newExpirationDate);

                    System.out.println("Назначение продлено успешно");
                });

        parser.registerCommand("assignment-search", "поиск назначений по фильтрам", (scanner, system) -> {
            Scanner inputScanner = new Scanner(System.in);

            System.out.println("Меню фильтров:");
            System.out.println("""
                        1. По пользователю
                        2. По роли
                        3. По типу
                        4. Активные
                        5. Неактивные
                        6. После даты
                        7. До даты
                        """);

            int selectedFilterNumber = 0;
            while (true) {
                try {
                    selectedFilterNumber = ConsoleUtils.promptInt(inputScanner, "Выберите номер фильтра:", 1, 7);
                    break;
                } catch (Exception e) {
                    System.out.println("Выбранный номер должен быть в диапазоне от " + 1 + " до " + 7);
                }
            }
            AssignmentFilter filter = switch (selectedFilterNumber) {
                case 1 -> {
                    parser.executeCommand("user-list", inputScanner, system);
                    String username = ConsoleUtils.promptString(inputScanner, "Имя пользователя:", true);
                    Optional<User> optionalUser = system.getUserManager().findByUsername(username);
                    if (optionalUser.isEmpty()) {
                        yield null;
                    }
                    yield AssignmentFilters.byUser(optionalUser.get());
                }
                case 2 -> {
                    String roleName = ConsoleUtils.promptString(inputScanner, "Роль:", true);
                    Optional<Role> optionalRole = system.getRoleManager().findByName(roleName);
                    if (optionalRole.isEmpty()) {
                        yield null;
                    }
                    yield AssignmentFilters.byRole(optionalRole.get());
                }
                case 3 -> AssignmentFilters.byType(ConsoleUtils.promptString(inputScanner, "Тип:", true));
                case 4 -> AssignmentFilters.activeOnly();
                case 5 -> AssignmentFilters.inactiveOnly();
                case 6 -> AssignmentFilters.assignedAfter(ConsoleUtils.promptString(inputScanner, "Дата:", true));
                case 7 -> AssignmentFilters.expiringBefore(ConsoleUtils.promptString(inputScanner, "Дата:", true));
                default -> null;
            };

            List<RoleAssignment> results = new ArrayList<>();
            if (filter != null) {
                results = system.getAssignmentManager().findByFilter(filter);
            }
            if (results.isEmpty()) {
                System.out.println("По данным фильтрам ничего не найдено");
                return;
            }

            System.out.println("Найденные назначения:");
            int n = 1;
            for (RoleAssignment roleAssignment : results) {
                System.out.printf("\t%d. %s\n", n, roleAssignment.summary(1));
                ++n;
            }
        });
    }
}