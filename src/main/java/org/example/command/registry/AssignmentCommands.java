package org.example.command.registry;

import de.vandermeer.asciitable.AsciiTable;
import org.example.assignment.*;
import org.example.command.CommandParser;
import org.example.role.Role;
import org.example.user.User;
import org.example.util.InputUtils;

import java.time.LocalDateTime;
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
                    System.out.println("Введите username пользователя, которому хотите назначить роль:");
                    username = inputScanner.nextLine(); // TODO

                    parser.executeCommand("role-list", inputScanner, system);
                    System.out.println("Введите номер желаемой роли:");
                    int selectedRoleNumber = Integer.parseInt(inputScanner.nextLine());
                    int selectedRoleIdx = selectedRoleNumber - 1;

                    int selectedAssignmentType = InputUtils.readInt(inputScanner, "Выберите тип назначения (0 - постоянное, 1 - временное):"); // TODO: проверка что это 0 или 1

                    String expirationDate = null;
                    if (selectedAssignmentType == 1) {
                        System.out.println("Введите дату истечения (формат yyyy-MM-dd HH:mm):");
                        expirationDate = inputScanner.nextLine();
                    }

                    System.out.println("Укажите причину назначения:");
                    String reason = inputScanner.nextLine();

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

                    String username = InputUtils.readLine(inputScanner,
                            "Введите username пользователя у которого хотите отзвать роль:");

                    parser.executeCommand("assignment-list-user", inputScanner, system); // TODO

                    int selectedAssignmentNumber = InputUtils.readInt(inputScanner,
                            "Введите номер назначения, которое хотите отозвать:");
                    int selectedAssignmentIdx = selectedAssignmentNumber - 1;

                    List<RoleAssignment> roleAssignmentList = system.getAssignmentManager().findAll();
                    RoleAssignment selectedAssignment = roleAssignmentList.get(selectedAssignmentIdx);

                    system.getAssignmentManager().revokeAssignment(selectedAssignment.assignmentId());

                    System.out.println("Роль успешно отозвана.");

                    system.getAuditLog().log("REVOKE_ROLE", system.getCurrentUser(),
                            username, null);
                });

        parser.registerCommand("assignment-list",
                "список всех назначений",
                (scanner, system) -> {
                    List<RoleAssignment> roleAssignmentList = system.getAssignmentManager().findAll();
                    AsciiTable table = new AsciiTable();

                    // table header
                    table.addRule();
                    table.addRow("username", "role", "type", "status", "assigned at", "assignment ID");
                    table.addRule();

                    // table rows
                    for (RoleAssignment roleAssignment : roleAssignmentList) {
                        String username = roleAssignment.user().username();
                        Role role = roleAssignment.role();
                        String type = roleAssignment.assignmentType();
                        String status = roleAssignment.isActive() ? "Active" : "Inactive";
                        String assignedAt = roleAssignment.metadata().assignedAt();
                        String assignmentId = roleAssignment.assignmentId();


                        table.addRow(username, role.toString(), type, status, assignedAt, assignmentId);
                        table.addRule();
                    }


                    System.out.println(table.render());
                });

        parser.registerCommand("assignment-list-user",
                "назначения конкретного пользователя",
                (scanner, system) -> {
                    Scanner inputScanner = new Scanner(System.in);

                    String username = InputUtils.readLine(inputScanner,
                            "Введите username:");

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

                    String roleName = InputUtils.readLine(inputScanner,
                            "Введите название роли:");

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
                        System.out.printf("\t%d. %s\n", n, roleAssignment.user().format());
                        ++n;
                    }
                });

        parser.registerCommand("assignment-expired",
                "истёкшие временные назначения",
                (scanner, system) -> {
                    String now = LocalDateTime.now().toString();
                    List<RoleAssignment> roleAssignmentList =
                            system.getAssignmentManager().findByFilter(AssignmentFilters.expiringBefore(now));

                    if (roleAssignmentList.isEmpty()) {
                        System.out.println("Нет ни одного истекшего временного назначения");
                        return;
                    }

                    System.out.println("Список истекших временных назначений");
                    int n = 1;
                    for (RoleAssignment roleAssignment : roleAssignmentList) {
                        System.out.printf("\t%d. %s\n", n, roleAssignment.user().format());
                        ++n;
                    }
                });

        parser.registerCommand("assignment-extend",
                "продлить временное назначение",
                (scanner, system) -> {
                    Scanner inputScanner = new Scanner(System.in);

                    String assignmentID = InputUtils.readLine(inputScanner,
                            "Введите assignment id:");

                    String newExpirationDate = InputUtils.readDateInString(inputScanner,
                            "Введите новый deadline:");

                    system.getAssignmentManager().extendTemporaryAssignment(assignmentID, newExpirationDate);
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

            AssignmentFilter filter = switch (InputUtils.readInt(inputScanner, "Выберите номер фильтра:")) {
                case 1 -> {
                    parser.executeCommand("user-list", inputScanner, system);
                    String username = InputUtils.readLine(inputScanner, "Имя пользователя:");
                    Optional<User> optionalUser = system.getUserManager().findByUsername(username);
                    if (optionalUser.isEmpty()) {
                        yield null;
                    }
                    yield AssignmentFilters.byUser(optionalUser.get());
                }
                case 2 -> {
                    String roleName = InputUtils.readLine(inputScanner, "Роль:");
                    Optional<Role> optionalRole = system.getRoleManager().findByName(roleName);
                    if (optionalRole.isEmpty()) {
                        yield null;
                    }
                    yield AssignmentFilters.byRole(optionalRole.get());
                }
                case 3 -> AssignmentFilters.byType(InputUtils.readLine(inputScanner, "Тип:"));
                case 4 -> AssignmentFilters.activeOnly();
                case 5 -> AssignmentFilters.inactiveOnly();
                case 6 -> AssignmentFilters.assignedAfter(InputUtils.readLine(inputScanner, "Дата:"));
                case 7 -> AssignmentFilters.expiringBefore(InputUtils.readLine(inputScanner, "Дата:"));
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