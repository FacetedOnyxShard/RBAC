package org.example;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Function;
import java.util.stream.IntStream;

public class CommandRegistry {
    private final CommandParser parser  = new CommandParser();

    public CommandRegistry() {
        new UserCommands(parser);
        new RoleCommands(parser);
        new AssignmentCommands(parser);
        new PermissionCommands(parser);
        new UtilityCommands(parser);
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
                            String username = get(Field.USERNAME, scanner);
                            String fullName = get(Field.FULL_NAME, scanner);
                            String email = get(Field.EMAIL, scanner);

                            try {
                                newUser = new User(username, fullName, email);
                                break;
                            } catch (Exception e) {
                                System.out.println("Не получилось создать пользователя. Ошибка: " + e.getMessage());
                                continue;
                            }
                        }

                        system.userManager.add(newUser);
                        System.out.println("Пользователь создан успешно.");
                    });

            parser.registerCommand("user-view",
                    "просмотр информации о пользователе",
                    ((scanner, system) -> {
                        String username = get(Field.USERNAME, scanner);

                        Optional<User> userOptional =  system.userManager.findByUsername(username);

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
                                String username = get(Field.USERNAME, scanner);
                                System.out.println("Новые данные пользователя");
                                String fullName = get(Field.FULL_NAME, scanner);
                                String email = get(Field.EMAIL, scanner);
                                system.userManager.update(username, fullName, email);
                                break;
                            } catch (Exception e) {
                                System.out.println("Не удалось обновить пользователя. Ошибка " + e.getMessage());
                                continue;
                            }
                        }
                    });

            parser.registerCommand("user-delete",
                    "удалить пользователя",
                    (scanner, system) -> {
                        String username = get(Field.USERNAME, scanner);

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

                        int selectedFilterNumber = scanner.nextInt();

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

        private static List<RoleAssignment> findUserAssignments(RBACSystem system, User user) {
            AssignmentFilter filterAssignmentByUser = AssignmentFilters.byUser(user);
            return system.assignmentManager.findByFilter(filterAssignmentByUser);
        }

        private static boolean prompt(Scanner scanner) {
            System.out.println("Вы уверены да/нет?");
            String answer = scanner.nextLine().trim();
            return answer.equals("да");
        }

        private enum Field {
            USERNAME(1, "Введите username: ", UserFilters::byUsernameContains),
            FULL_NAME(2, "Введите полное имя: ", UserFilters::byFullNameContains),
            EMAIL(3,"Введите email: ", UserFilters::byEmail),
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
                        for (Role role : system.roleManager.findAll()) {
                            System.out.println(role.toString() + " " + role.getPermissions().size());
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

                                while(prompt(scanner, "Хотите добавить новое право")) {
                                    parser.executeCommand("role-add-permission", scanner, system);
                                }
                                break;
                            } catch (Exception e) {
                                System.out.println("Не удалось создать роль. Ошибка " + e.getMessage());
                                continue;
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
                        role.name = name;
                        role.description = description;
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
                                continue;
                            }
                        }


                        system.roleManager.addPermissionToRole(role.name, permission);
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
                        for (Permission permission: permissions) {
                            System.out.printf("\t%d. %s", i, permission.format());
                            ++i;
                        }

                        System.out.println("Введите номер права для удаления:");
                        int permissionForRemoveNumber = scanner.nextInt();
                        if (1 <= permissionForRemoveNumber && permissionForRemoveNumber >= permissions.length) {
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
                    });
        }

        private static boolean prompt(Scanner scanner, String question) {
            if (question == null) {
                return prompt(scanner);
            }
            System.out.println(question + " да/нет?");
            String answer = scanner.nextLine().trim();
            return answer.equals("да");
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
                        // TODO: Implement assign-role command
                    });

            parser.registerCommand("revoke-role",
                    "отозвать роль у пользователя",
                    (scanner, system) -> {
                        // TODO: Implement revoke-role command
                    });

            parser.registerCommand("assignment-list",
                    "список всех назначений",
                    (scanner, system) -> {
                        // TODO: Implement assignment-list command
                    });

            parser.registerCommand("assignment-list-user",
                    "назначения конкретного пользователя",
                    (scanner, system) -> {
                        // TODO: Implement assignment-list-user command
                    });

            parser.registerCommand("assignment-list-role",
                    "список пользователей с конкретной ролью",
                    (scanner, system) -> {
                        // TODO: Implement assignment-list-role command
                    });

            parser.registerCommand("assignment-active",
                    "только активные назначения",
                    (scanner, system) -> {
                        // TODO: Implement assignment-active command
                    });

            parser.registerCommand("assignment-expired",
                    "истёкшие временные назначения",
                    (scanner, system) -> {
                        // TODO: Implement assignment-expired command
                    });

            parser.registerCommand("assignment-extend",
                    "продлить временное назначение",
                    (scanner, system) -> {
                        // TODO: Implement assignment-extend command
                    });

            parser.registerCommand("assignment-search",
                    "поиск назначений по фильтрам",
                    (scanner, system) -> {
                        // TODO: Implement assignment-search command
                    });
        }
    }

    private static class PermissionCommands {
        public PermissionCommands(CommandParser parser) {
            parser.registerCommand("permissions-user",
                    "все права конкретного пользователя",
                    (scanner, system) -> {
                        // TODO: Implement permissions-user command
                    });

            parser.registerCommand("permissions-check",
                    "проверить, есть ли у пользователя конкретное право",
                    (scanner, system) -> {
                        // TODO: Implement permissions-check command
                    });
        }
    }

    private static class UtilityCommands {
        public UtilityCommands(CommandParser parser) {
            parser.registerCommand("help",
                    "справка по командам",
                    (scanner, system) -> {
                        // TODO: Implement help command (using CommandParser.printHelp())
                    });

            parser.registerCommand("stats",
                    "статистика системы",
                    (scanner, system) -> {
                        // TODO: Implement stats command (using RBACSystem.generateStatistics())
                    });

            parser.registerCommand("clear",
                    "очистить экран",
                    (scanner, system) -> {
                        // TODO: Implement clear command
                    });

            parser.registerCommand("exit",
                    "выход из программы",
                    (scanner, system) -> {
                        // TODO: Implement exit command
                    });

            parser.registerCommand("save",
                    "сохранить данные в файл",
                    (scanner, system) -> {
                        // TODO: Implement save command
                    });

            parser.registerCommand("load",
                    "загрузить данные из файла",
                    (scanner, system) -> {
                        // TODO: Implement load command
                    });
        }
    }
}
