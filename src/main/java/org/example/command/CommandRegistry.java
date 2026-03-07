package org.example.command;

import de.vandermeer.asciitable.AsciiTable;
import org.example.assignment.*;
import org.example.assignment.PermanentAssignment;
import org.example.core.Permission;
import org.example.core.RBACSystem;
import org.example.role.Role;
import org.example.assignment.TemporaryAssignment;
import org.example.user.User;
import org.example.user.UserFilter;
import org.example.user.UserFilters;
import org.example.util.InputUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.IntStream;

public class CommandRegistry {
    public CommandRegistry(CommandParser parser) {
        new UserCommands(parser);
        new RoleCommands(parser);
        new AssignmentCommands(parser);
        new PermissionCommands(parser);
        new UtilityCommands(parser);
    }

    private static List<RoleAssignment> findUserAssignments(RBACSystem system, User user) {
        AssignmentFilter filterAssignmentByUser = AssignmentFilters.byUser(user);
        return system.assignmentManager.findByFilter(filterAssignmentByUser);
    }

    private static class UserCommands {
        public UserCommands(CommandParser parser) {
            parser.registerCommand("user-list",
                    "вывести список всех пользователей",
                    (scanner, system) -> {
                        if (!scanner.hasNext()) {
                            System.out.println("Список всех пользователей:");
                            for (User user : system.userManager.findAll()) {
                                user.format();
                            }
                        } else {
                            parser.executeCommand("user-search", scanner, system);
                        }
                    });

            parser.registerCommand("user-create",
                    "создать нового пользователя",
                    (scanner, system) -> {
                        User newUser;
                        while (true) {
                            String username = InputUtils.readLine(scanner, Field.USERNAME.getMessage());
                            String fullName = InputUtils.readLine(scanner, Field.FULL_NAME.getMessage());
                            String email = InputUtils.readLine(scanner, Field.EMAIL.getMessage());

                            try {
                                newUser = new User(username, fullName, email);
                                break;
                            } catch (Exception e) {
                                System.out.println("Не получилось создать пользователя. Ошибка: " + e.getMessage());
                            }
                        }

                        system.userManager.add(newUser);
                        System.out.println("Пользователь создан успешно.");
                    });

            parser.registerCommand("user-view",
                    "просмотр информации о пользователе",
                    ((scanner, system) -> {
                        String username = InputUtils.readLine(scanner, Field.USERNAME.getMessage());

                        Optional<User> userOptional = system.userManager.findByUsername(username);

                        if (userOptional.isPresent()) {
                            User user = userOptional.get();

                            List<RoleAssignment> roleAssignmentList = findUserAssignments(system, user);

                            System.out.println("Информация о пользователе");
                            System.out.println("\tОбщая информация: " + user.format());

                            System.out.println("\tСписок назначенных ролей: ");
                            IntStream.range(0, roleAssignmentList.size())
                                    .forEach(i -> System.out.println("\t\t" + (i + 1) + ". " + roleAssignmentList.get(i)));

                            System.out.println("\tПрава пользователя: ");
                            Permission[] currentRolePermissions;
                            for (int i = 0; i < roleAssignmentList.size(); ++i) {
                                currentRolePermissions =
                                        roleAssignmentList.get(i).role().getPermissions().toArray(new Permission[0]);

                                for (int j = 0; j < currentRolePermissions.length; ++j) {
                                    System.out.println("\t\t" + (i + j + 1) + ". " + currentRolePermissions[j]);
                                }
                            }
                        } else {
                            System.out.println("Пользователь с username = " + username + " не найден");
                        }
                    }));

            parser.registerCommand("user-update",
                    "обновить данные пользователя",
                    (scanner, system) -> {
                        while (true) {
                            try {
                                String username = InputUtils.readLine(scanner, Field.USERNAME.getMessage());
                                System.out.println("Новые данные пользователя");
                                String fullName = InputUtils.readLine(scanner, Field.FULL_NAME.getMessage());
                                String email = InputUtils.readLine(scanner, Field.EMAIL.getMessage());
                                system.userManager.update(username, fullName, email);
                                break;
                            } catch (Exception e) {
                                System.out.println("Не удалось обновить пользователя. Ошибка " + e.getMessage());
                            }
                        }
                    });

            parser.registerCommand("user-delete",
                    "удалить пользователя",
                    (scanner, system) -> {
                        String username = InputUtils.readLine(scanner, Field.USERNAME.getMessage());

                        if (prompt(scanner)) {
                            Optional<User> optionalUser = system.userManager.findByUsername(username);

                            if (optionalUser.isEmpty()) {
                                System.out.println("Пользователь не был найден");
                                return;
                            }

                            User user = optionalUser.get();
                            List<RoleAssignment> roleAssignmentList = findUserAssignments(system, user);

                            for (RoleAssignment roleAssignment : roleAssignmentList) {
                                system.assignmentManager.remove(roleAssignment);
                            }
                            system.userManager.remove(user);

                        } else {
                            System.out.println("Действие было отменено пользователем");
                        }
                    });

            parser.registerCommand("user-search",
                    "поиск пользователей по фильтрам",
                    (scanner, system) -> {
                        System.out.println("Выберите фильтр (для выбора введите цифру)");
                        String[] options = {"by username", "by email", "by email domain", "by full name"};
                        int i = 1;
                        for (String option : options) {
                            System.out.println("\t" + i + ". " + option);
                            ++i;
                        }

                        int selectedFilterNumber = Integer.parseInt(scanner.nextLine());

                        Field field = Field.fromNumber(selectedFilterNumber);
                        String filterValue = get(field, scanner);
                        UserFilter filter = field.createFilter(filterValue);

                        List<User> userList = system.userManager.findByFilter(filter);

                        if (userList.isEmpty()) {
                            System.out.println("Пользователей по данным фильтрам не было найдено");
                        }
                        System.out.println("Найденные пользователи:");
                        for (User user : userList) {
                            System.out.println("\t" + user.format());
                        }
                    });
        }

        private static boolean prompt(Scanner scanner) {
            System.out.println("Вы уверены да/нет?");
            String answer = scanner.nextLine().trim();
            return answer.equals("да");
        }

        private enum Field {
            USERNAME(1, "Введите username: ", UserFilters::byUsernameContains),
            FULL_NAME(2, "Введите полное имя: ", UserFilters::byFullNameContains),
            EMAIL(3, "Введите email: ", UserFilters::byEmail),
            EMAIL_DOMAIN(4, "Введите домен email: ", UserFilters::byEmailDomain);

            private final int number;
            private final String message;
            private final Function<String, UserFilter> filterCreator;

            Field(int number, String message, Function<String, UserFilter> filterCreator) {
                this.number = number;
                this.message = message;
                this.filterCreator = filterCreator;
            }

            public String getMessage() {
                return message;
            }

            public UserFilter createFilter(String value) {
                return filterCreator.apply(value);
            }

            public static Field fromNumber(int n) {
                for (Field field : values()) {
                    if (field.number == n) {
                        return field;
                    }
                }
                throw new IllegalArgumentException("Invalid filter number " + n);
            }
        }

        private static String get(Field field, Scanner scanner) {
            System.out.println(field.getMessage());
            return scanner.nextLine();
        }
    }

    private static class RoleCommands {
        public RoleCommands(CommandParser parser) {
            parser.registerCommand("role-list",
                    "вывести список всех ролей",
                    (scanner, system) -> {
                        System.out.println("Список ролей:");
                        int n = 1;
                        for (Role role : system.roleManager.findAll()) {
                            System.out.printf("\t%d. %s %d\n", n, role.toString(), role.getPermissions().size());
                            ++n;
                        }
                    });

            parser.registerCommand("role-create",
                    "создать новую роль",
                    (scanner, system) -> {
                        Role newRole;
                        while (true) {
                            try {
                                String name = getName(scanner);
                                String description = getDescription(scanner);

                                newRole = new Role(name, description);
                                system.roleManager.add(newRole);

                                while (InputUtils.confirm(scanner, "Хотите добавить новое право")) {
                                    parser.executeCommand("role-add-permission", scanner, system);
                                }
                                break;
                            } catch (Exception e) {
                                System.out.println("Не удалось создать роль. Ошибка " + e.getMessage());
                            }
                        }
                    });

            parser.registerCommand("role-view",
                    "просмотр роли",
                    (scanner, system) -> {
                        String name = getName(scanner);

                        Optional<Role> optionalRole = system.roleManager.findByName(name);
                        if (optionalRole.isEmpty()) {
                            System.out.println("Роль с именем " + name + " не была найдена");
                            return;
                        }

                        Role role = optionalRole.get();
                        System.out.println(role.format());
                    });

            parser.registerCommand("role-update",
                    "обновить роль (название/описание)",
                    (scanner, system) -> {
                        String name = getName(scanner);
                        String description = getDescription(scanner);

                        Optional<Role> optionalRole = system.roleManager.findByName(name);
                        if (optionalRole.isEmpty()) {
                            System.out.println("Роль с именем " + name + " не была найдена");
                            return;
                        }

                        Role role = optionalRole.get();
                        role.setName(name);
                        role.setDescription(description);
                    });

            parser.registerCommand("role-delete",
                    "удалить роль",
                    (scanner, system) -> {
                        String name = getName(scanner);

                        Optional<Role> optionalRole = system.roleManager.findByName(name);
                        if (optionalRole.isEmpty()) {
                            System.out.println("Роль с именем " + name + " не была найдена");
                            return;
                        }

                        Role role = optionalRole.get();

                        List<RoleAssignment> roleAssignmentList = system.assignmentManager.findByRole(role);

                        if (!roleAssignmentList.isEmpty()) {
                            System.out.println("Не удалось удалить роль. Причина: она назначена пользователям");
                            System.out.println("Список пользователей, которым она назначена:");
                            int i = 1;
                            for (RoleAssignment roleAssignment : roleAssignmentList) {
                                System.out.println("\t" + i + ". " + roleAssignment.user());
                                ++i;
                            }
                            return;
                        }

                        if (prompt(scanner)) {
                            system.roleManager.remove(role);
                            System.out.println("Роль успешно удалена");
                        } else {
                            System.out.println("Отмена удаления роли...");
                        }
                    });

            parser.registerCommand("role-add-permission",
                    "добавить право к роли",
                    (scanner, system) -> {
                        String name = getName(scanner);

                        Optional<Role> optionalRole = system.roleManager.findByName(name);
                        if (optionalRole.isEmpty()) {
                            System.out.println("Роль с именем " + name + " не была найдена");
                            return;
                        }

                        Role role = optionalRole.get();
                        Permission permission;

                        while (true) {
                            System.out.println("Введите данные права");
                            System.out.println("Имя:");
                            String permissionName = scanner.nextLine();
                            System.out.println("Ресурс:");
                            String resource = scanner.nextLine();
                            System.out.println("Описание:");
                            String description = scanner.nextLine();

                            try {
                                permission = new Permission(permissionName, resource, description);
                                break;
                            } catch (Exception e) {
                                System.out.println("Не удалось создать роль. Ошибка: " + e.getMessage());
                            }
                        }


                        system.roleManager.addPermissionToRole(role.getName(), permission);
                    });

            parser.registerCommand("role-remove-permission",
                    "удалить право из роли",
                    (scanner, system) -> {
                        String name = getName(scanner);

                        Optional<Role> optionalRole = system.roleManager.findByName(name);
                        if (optionalRole.isEmpty()) {
                            System.out.println("Роль с именем " + name + " не была найдена");
                            return;
                        }

                        Role role = optionalRole.get();
                        Permission[] permissions = role.getPermissions().toArray(new Permission[0]);
                        int i = 1;
                        System.out.println("Права роли:");
                        for (Permission permission : permissions) {
                            System.out.printf("\t%d. %s\n", i, permission.format());
                            ++i;
                        }

                        System.out.println("Введите номер права для удаления:");
                        int permissionForRemoveNumber = Integer.parseInt(scanner.nextLine());
                        if (1 <= permissionForRemoveNumber && permissionForRemoveNumber <= permissions.length) {
                            int removeIdx = permissionForRemoveNumber - 1;
                            role.removePermission(permissions[removeIdx]);
                        } else {
                            System.out.println("Выбран некорректный номер");
                        }
                    });

            parser.registerCommand("role-search",
                    "поиск ролей",
                    (scanner, system) -> {
                        String name = getName(scanner);

                        Optional<Role> optionalRole = system.roleManager.findByName(name);
                        if (optionalRole.isEmpty()) {
                            System.out.println("Роль с именем " + name + " не была найдена");
                            return;
                        }

                        Role role = optionalRole.get();
                        System.out.println(role.toString());
                    });
        }

        private static boolean prompt(Scanner scanner) {
            System.out.println("Вы уверены да/нет?");
            String answer = scanner.nextLine().trim();
            return answer.equals("да");
        }

        private static String getName(Scanner scanner) {
            System.out.println("Введите название роли: ");
            return scanner.nextLine();
        }

        private static String getDescription(Scanner scanner) {
            System.out.println("Введите описание роли:");
            return scanner.nextLine();
        }
    }

    private static class AssignmentCommands {
        public AssignmentCommands(CommandParser parser) {
            parser.registerCommand("assign-role",
                    "назначить роль пользователю",
                    (scanner, system) -> {
                        String username;
                        System.out.println("Введите username:");
                        username = scanner.nextLine();

                        parser.executeCommand("role-list", scanner, system);
                        System.out.println("Введите номер желаемой роли:");
                        int selectedRoleNumber = Integer.parseInt(scanner.nextLine());
                        int selectedRoleIdx = selectedRoleNumber - 1;

                        System.out.println("Выберите тип назначения (0 - постоянное, 1 - временное):");
                        boolean selectedAssignmentType = scanner.nextBoolean();

                        String expirationDate = null;
                        if (selectedAssignmentType) {
                            System.out.println("Введите дату истечения");
                            expirationDate = scanner.nextLine();
                        }

                        System.out.println("Укажите причину назначения");
                        String reason = scanner.nextLine();

                        AbstractRoleAssignment newAssignment;
                        AssignmentMetadata metadata = AssignmentMetadata.now(system.getCurrentUser(), reason);
                        List<Role> roleList = system.roleManager.findAll();

                        Optional<User> optionalUser = system.userManager.findByUsername(username);
                        if (optionalUser.isEmpty()) {
                            System.out.println("Ошибка: не удалось найти пользователя с username = " + username);
                            return;
                        }
                        User user = optionalUser.get();
                        Role selectedRole = roleList.get(selectedRoleIdx);

                        if (selectedAssignmentType) {
                            newAssignment = new TemporaryAssignment(user, selectedRole, metadata, expirationDate, false);
                        } else {
                            newAssignment = new PermanentAssignment(user, selectedRole, metadata);
                        }
                        system.assignmentManager.add(newAssignment);
                    });

            parser.registerCommand("revoke-role",
                    "отозвать роль у пользователя",
                    (scanner, system) -> {
                        String username = InputUtils.readLine(scanner,
                                "Введите username:");

                        parser.executeCommand("assignment-list-user", scanner, system);
                        int selectedAssignmentNumber = InputUtils.readInt(scanner,
                                "Введите номер назначения, которое хотите отозвать:");
                        int selectedAssignmentIdx = selectedAssignmentNumber - 1;

                        List<RoleAssignment> roleAssignmentList = system.assignmentManager.findAll();
                        system.assignmentManager.remove(roleAssignmentList.get(selectedAssignmentIdx));
                    });

            parser.registerCommand("assignment-list",
                    "список всех назначений",
                    (scanner, system) -> {
                        List<RoleAssignment> roleAssignmentList = system.assignmentManager.findAll();
                        AsciiTable table = new AsciiTable();

                        // table header
                        table.addRule();
                        table.addRow("username", "role", "type", "status", "assigned at");
                        table.addRule();

                        // table rows
                        for (RoleAssignment roleAssignment : roleAssignmentList) {
                            String username = roleAssignment.user().username();
                            Role role = roleAssignment.role();
                            String type = roleAssignment.assignmentType();
                            String status = roleAssignment.isActive() ? "Active" : "Inactive";
                            String assignedAt = roleAssignment.metadata().assignedAt();


                            table.addRow(username, role.toString(), type, status, assignedAt);
                            table.addRule();
                        }


                        System.out.println(table.render());
                    });

            parser.registerCommand("assignment-list-user",
                    "назначения конкретного пользователя",
                    (scanner, system) -> {
                        String username = InputUtils.readLine(scanner,
                                "Введите username:");

                        List<RoleAssignment> roleAssignmentList =
                                system.assignmentManager.findByFilter(AssignmentFilters.byUsername(username));
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
                        String roleName = InputUtils.readLine(scanner,
                                "Введите название роли:");

                        List<RoleAssignment> roleAssignmentList =
                                system.assignmentManager.findByFilter(AssignmentFilters.byRoleName(roleName));
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
                                system.assignmentManager.findByFilter(AssignmentFilters.activeOnly());
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
                                system.assignmentManager.findByFilter(AssignmentFilters.expiringBefore(now));
                        int n = 1;
                        for (RoleAssignment roleAssignment : roleAssignmentList) {
                            System.out.printf("\t%d. %s\n", n, roleAssignment.user().format());
                            ++n;
                        }
                    });

            parser.registerCommand("assignment-extend",
                    "продлить временное назначение",
                    (scanner, system) -> {
                        String assignmentID = InputUtils.readLine(scanner,
                                "Введите assignment id:");

                        String newExpirationDate = InputUtils.readDateInString(scanner,
                                "Введите новый deadline:");

                        system.assignmentManager.extendTemporaryAssignment(assignmentID, newExpirationDate);
                    });

            parser.registerCommand("assignment-search", "поиск назначений по фильтрам", (scanner, system) -> {
                System.out.println("""
                        1. По пользователю
                        "2. По роли
                        "3. По типу
                        "4. Активные
                        "5. Неактивные
                        "6. После даты
                        "7. До даты
                        """);

                AssignmentFilter filter = switch (InputUtils.readInt(scanner, "Выберите:")) {
                    case 1 -> {
                        String username = InputUtils.readLine(scanner, "Имя:");
                        Optional<User> optionalUser = system.userManager.findByUsername(username);
                        if (optionalUser.isEmpty()) {
                            yield null;
                        }
                        yield AssignmentFilters.byUser(optionalUser.get());
                    }
                    case 2 -> {
                        String roleName = InputUtils.readLine(scanner, "Роль:");
                        Optional<Role> optionalRole = system.roleManager.findByName(roleName);
                        if (optionalRole.isEmpty()) {
                            yield null;
                        }
                        yield AssignmentFilters.byRole(optionalRole.get());
                    }
                    case 3 -> AssignmentFilters.byType(InputUtils.readLine(scanner, "Тип:"));
                    case 4 -> AssignmentFilters.activeOnly();
                    case 5 -> AssignmentFilters.inactiveOnly();
                    case 6 -> AssignmentFilters.assignedAfter(InputUtils.readLine(scanner, "Дата:"));
                    case 7 -> AssignmentFilters.expiringBefore(InputUtils.readLine(scanner, "Дата:"));
                    default -> null;
                };

                List<RoleAssignment> results = new ArrayList<>();
                if (filter != null) {
                    results = system.assignmentManager.findByFilter(filter);
                }
                System.out.println(results.isEmpty() ? "Ничего не найдено" : results);
            });
        }
    }

    private static class PermissionCommands {
        public PermissionCommands(CommandParser parser) {
            parser.registerCommand("permissions-user",
                    "все права конкретного пользователя",
                    (scanner, system) -> {
                        String username = InputUtils.readLine(scanner, "Введите username:");

                        Optional<User> optionalUser = system.userManager.findByUsername(username);
                        if (optionalUser.isEmpty()) {
                            System.out.println("Пользователь с ником " + username + " не найден");
                            return;
                        }
                        User user = optionalUser.get();

                        List<RoleAssignment> assignments = system.assignmentManager
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
                        String username = InputUtils.readLine(scanner,
                                "Введите username:");

                        String permissionName = InputUtils.readLine(scanner,
                                "Введите наименование права:");

                        String resource = InputUtils.readLine(scanner,
                                "Введите ресурс:");

                        Optional<User> optionalUser = system.userManager.findByUsername(username);
                        if (optionalUser.isEmpty()) {
                            System.out.println("Пользователь с ником " + username + " не найден");
                            return;
                        }
                        User user = optionalUser.get();

                        if (system.assignmentManager.userHasPermission(user, permissionName, resource)) {
                            System.out.println("YES. ");
                            for (RoleAssignment a : system.assignmentManager.findByFilter(AssignmentFilters.byUser(user))) {
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

    private static class UtilityCommands {
        public UtilityCommands(CommandParser parser) {
            parser.registerCommand("help",
                    "справка по командам",
                    (scanner, system) -> {
                        parser.printHelp();
                    });

            parser.registerCommand("stats",
                    "статистика системы",
                    (scanner, system) -> {
                        System.out.println(system.generateStatistics());
                    });

            parser.registerCommand("clear",
                    "очистить экран",
                    (scanner, system) -> {
                        System.out.print("\033[H\033[3J");
                        System.out.flush();
                    });

            parser.registerCommand("exit",
                    "выход из программы",
                    (scanner, system) -> {
                        scanner.close();
                        System.out.println("Остановка выполнения программы...");
                        System.exit(0);
                    });
        }
    }
}
