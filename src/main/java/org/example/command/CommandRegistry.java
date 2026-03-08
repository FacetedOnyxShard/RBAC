package org.example.command;

import de.vandermeer.asciitable.AsciiTable;
import org.example.assignment.*;
import org.example.assignment.PermanentAssignment;
import org.example.core.Permission;
import org.example.core.RBACSystem;
import org.example.role.Role;
import org.example.assignment.TemporaryAssignment;
import org.example.role.RoleFilter;
import org.example.role.RoleFilters;
import org.example.user.User;
import org.example.user.UserFilter;
import org.example.user.UserFilters;
import org.example.util.AuditLog;
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
        private static boolean commandWithoutFlags(Scanner scanner) {
            return !scanner.hasNext();
        }

        private void userListLogic(Scanner scanner, RBACSystem system) {
            if (commandWithoutFlags(scanner)) {
                System.out.println("Список всех пользователей:");
                int n = 1;
                for (User user : system.userManager.findAll()) {
                    System.out.printf("\t%d. %s\n", n, user.format());
                    ++n;
                }
                return;
            }

            HashSet<String> availableFlags = new HashSet<>(Set.of("username", "email", "name", "domain"));
            List<UsernameFieldHelper> fields = new ArrayList<>();
            List<String> values = new ArrayList<>();

            while (scanner.hasNext()) {
                String currentParam = scanner.next();
                if (currentParam.length() > 2 && currentParam.startsWith("--")) {
                    String flagValue = currentParam.substring(2);

                    if (!availableFlags.contains(flagValue)) {
                        throw new RuntimeException();
                    }
                    if (!scanner.hasNext()) {
                        throw new RuntimeException();
                    }

                    String value = scanner.next();

                    if (value.isBlank()) {
                        throw new RuntimeException();
                    }

                    fields.add(UsernameFieldHelper.fromFlag(flagValue));
                    values.add(value);
                }
            }

            searchLogicForManyFilters(fields, values, system);
        }

        private void userCreateLogic(Scanner scanner, RBACSystem system) {
            Scanner inputScanner = new Scanner(System.in);

            User newUser;
            while (true) {
                String username = InputUtils.readLine(inputScanner, UsernameFieldHelper.USERNAME.getMessage());
                String fullName = InputUtils.readLine(inputScanner, UsernameFieldHelper.FULL_NAME.getMessage());
                String email = InputUtils.readLine(inputScanner, UsernameFieldHelper.EMAIL.getMessage());

                try {
                    newUser = new User(username, fullName, email);
                    break;
                } catch (Exception e) {
                    System.out.println("Не получилось создать пользователя. Ошибка: " + e.getMessage());
                }
            }

            system.userManager.add(newUser);
            System.out.println("Пользователь создан успешно.");
        }

        private void userViewLogic(Scanner scanner, RBACSystem system)   {
            Scanner inputScanner = new Scanner(System.in);
            String username = InputUtils.readLine(inputScanner, UsernameFieldHelper.USERNAME.getMessage());

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
        }

        public UserCommands(CommandParser parser) {
            parser.registerCommand("user-list",
                    """
                    вывести список всех пользователей
                    \t\tсписок доступных аргументов:
                    \t\t\t--username <username>
                    \t\t\t--email <email>
                    \t\t\t--domain <email domain>
                    \t\t\t--name <full name>
                    """,
                    this::userListLogic);

            parser.registerCommand("user-create",
                    "создать нового пользователя",
                    this::userCreateLogic);

            parser.registerCommand("user-view",
                    "просмотр информации о пользователе",
                    this::userViewLogic);

            parser.registerCommand("user-update",
                    "обновить данные пользователя",
                    (scanner, system) -> {
                        Scanner inputScanner = new Scanner(System.in);
                        while (true) {
                            try {
                                String username = InputUtils.readLine(inputScanner, UsernameFieldHelper.USERNAME.getMessage());
                                System.out.println("Новые данные пользователя");
                                String fullName = InputUtils.readLine(inputScanner, UsernameFieldHelper.FULL_NAME.getMessage());
                                String email = InputUtils.readLine(inputScanner, UsernameFieldHelper.EMAIL.getMessage());
                                system.userManager.update(username, fullName, email);
                                break;
                            } catch (Exception e) {
                                System.out.println("Не удалось обновить пользователя. Ошибка " + e.getMessage());
                            }
                        }
                        System.out.println("Данные пользователя успешно обновлены");
                    });

            parser.registerCommand("user-delete",
                    "удалить пользователя",
                    (scanner, system) -> {
                        Scanner inputScanner = new Scanner(System.in);
                        String username = InputUtils.readLine(inputScanner, UsernameFieldHelper.USERNAME.getMessage());

                        if (prompt(inputScanner)) {
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

                            System.out.println("Пользователь успешно удален");
                        } else {
                            System.out.println("Действие было отменено пользователем");
                        }
                    });

            parser.registerCommand("user-search",
                    "поиск пользователей по фильтрам",
                    (scanner, system) -> {
                        Scanner inputScanner = new Scanner(System.in);
                        System.out.println("Выберите фильтр (для выбора введите цифру)");
                        String[] options = {"by username", "by full name", "by email", "by email domain"};
                        int i = 1;
                        for (String option : options) {
                            System.out.println("\t" + i + ". " + option);
                            ++i;
                        }

                        int selectedFilterNumber = Integer.parseInt(inputScanner.nextLine());

                        searchLogic(selectedFilterNumber, inputScanner, system);
                    });
        }

        private static void searchLogic(int selectedFilterNumber, Scanner scanner, RBACSystem system) {
            UsernameFieldHelper usernameFieldHelper = UsernameFieldHelper.fromNumber(selectedFilterNumber);
            String filterValue = get(usernameFieldHelper, scanner);
            UserFilter filter = usernameFieldHelper.createFilter(filterValue);

            List<User> userList = system.userManager.findByFilter(filter);

            if (userList.isEmpty()) {
                System.out.println("Пользователей по данным фильтрам не было найдено");
                return;
            }
            System.out.println("Найденные пользователи:");
            int n = 1;
            for (User user : userList) {
                System.out.printf("\t%d. %s\n", n, user.format());
                ++n;
            }
        }

        private static void searchLogicForManyFilters(List<UsernameFieldHelper> usernameFieldHelpers, List<String> filterValues, RBACSystem system) {
            List<User> userList = system.userManager.findAll();

            for (int i = 0; i < usernameFieldHelpers.size(); ++i) {
                UsernameFieldHelper usernameFieldHelper = usernameFieldHelpers.get(i);
                UserFilter filter = usernameFieldHelper.createFilter(filterValues.get(i));
                userList = userList.stream().filter(filter::test).toList();
            }

            if (userList.isEmpty()) {
                System.out.println("Пользователей по данным фильтрам не было найдено");
                return;
            }
            System.out.println("Найденные пользователи:");
            int n = 1;
            for (User user : userList) {
                System.out.printf("\t%d. %s\n", n, user.format());
                ++n;
            }
        }

        private static boolean prompt(Scanner scanner) {
            System.out.println("Вы уверены да/нет?");
            String answer = scanner.nextLine().trim();
            return answer.equals("да");
        }

        private enum UsernameFieldHelper {
            USERNAME(1, "Введите username: ", UserFilters::byUsernameContains, "username"),
            FULL_NAME(2, "Введите полное имя: ", UserFilters::byFullNameContains, "name"),
            EMAIL(3, "Введите email: ", UserFilters::byEmailContains, "email"),
            EMAIL_DOMAIN(4, "Введите домен email: ", UserFilters::byEmailDomain, "domain");

            private final int number;
            private final String message;
            private final Function<String, UserFilter> filterCreator;
            private final String flag;

            UsernameFieldHelper(int number, String message, Function<String, UserFilter> filterCreator, String flag) {
                this.number = number;
                this.message = message;
                this.filterCreator = filterCreator;
                this.flag = flag;
            }

            public String getMessage() {
                return message;
            }

            public UserFilter createFilter(String value) {
                return filterCreator.apply(value);
            }

            public static UsernameFieldHelper fromNumber(int n) {
                for (UsernameFieldHelper usernameFieldHelper : values()) {
                    if (usernameFieldHelper.number == n) {
                        return usernameFieldHelper;
                    }
                }
                throw new IllegalArgumentException("Invalid filter number " + n);
            }

            public static UsernameFieldHelper fromFlag(String flag) {
                for (UsernameFieldHelper usernameFieldHelper : values()) {
                    if (usernameFieldHelper.flag.equals(flag)) {
                        return usernameFieldHelper;
                    }
                }
                throw new IllegalArgumentException("Invalid flag " + flag);
            }
        }

        private static String get(UsernameFieldHelper usernameFieldHelper, Scanner scanner) {
            System.out.println(usernameFieldHelper.getMessage());
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
                            System.out.printf("\t%d. Название: %s Количество прав: %d ID: %s\n", n, role.toString(), role.getPermissions().size(), role.getId());
                            ++n;
                        }
                    });

            parser.registerCommand("role-create",
                    "создать новую роль",
                    (scanner, system) -> {
                        Scanner inputScanner = new Scanner(System.in);

                        Role newRole;
                        while (true) {
                            try {
                                String name = getName(inputScanner);
                                String description = getDescription(inputScanner);

                                newRole = new Role(name, description);
                                system.roleManager.add(newRole);

                                while (InputUtils.confirm(inputScanner, "Хотите добавить новое право")) {
                                    parser.executeCommand("role-add-permission", inputScanner, system);
                                }
                                break;
                            } catch (Exception e) {
                                System.out.println("Не удалось создать роль. Ошибка " + e.getMessage());
                            }
                        }
                        System.out.println("Новая роль успешно создана");
                    });

            parser.registerCommand("role-view",
                    "просмотр роли",
                    (scanner, system) -> {
                        Scanner inputScanner = new Scanner(System.in);

                        String name = getName(inputScanner);

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
                        Scanner inputScanner = new Scanner(System.in);

                        String name = getName(inputScanner);
                        String description = getDescription(inputScanner);

                        Optional<Role> optionalRole = system.roleManager.findByName(name);
                        if (optionalRole.isEmpty()) {
                            System.out.println("Роль с именем " + name + " не была найдена");
                            return;
                        }

                        String newName = getName(inputScanner);

                        Role role = optionalRole.get();
                        role.setName(newName);
                        role.setDescription(description);
                    });

            parser.registerCommand("role-delete",
                    "удалить роль",
                    (scanner, system) -> {
                        Scanner inputScanner = new Scanner(System.in);

                        String name = getName(inputScanner);

                        Optional<Role> optionalRole = system.roleManager.findByName(name);
                        if (optionalRole.isEmpty()) {
                            System.out.println("Роль с именем " + name + " не была найдена");
                            return;
                        }

                        Role role = optionalRole.get();

                        List<RoleAssignment> roleAssignmentList = system.assignmentManager.findByRole(role);

                        if (!roleAssignmentList.isEmpty()) {
                            System.out.println("Не удалось удалить роль. Причина: роль назначена пользователям");
                            System.out.println("Список пользователей, которым назначена роль:");
                            int i = 1;
                            for (RoleAssignment roleAssignment : roleAssignmentList) {
                                System.out.println("\t" + i + ". " + roleAssignment.user());
                                ++i;
                            }
                            return;
                        }

                        if (prompt(inputScanner)) {
                            system.roleManager.remove(role);
                            System.out.println("Роль успешно удалена");
                        } else {
                            System.out.println("Отмена удаления роли...");
                        }
                    });

            parser.registerCommand("role-add-permission",
                    "добавить право к роли",
                    (scanner, system) -> {
                        Scanner inputScanner = new Scanner(System.in);

                        String name = getName(inputScanner);

                        Optional<Role> optionalRole = system.roleManager.findByName(name);
                        if (optionalRole.isEmpty()) {
                            System.out.println("Роль с именем " + name + " не была найдена");
                            return;
                        }

                        Role role = optionalRole.get();
                        Permission permission;

                        while (true) {
                            System.out.println("Введите данные права");
                            System.out.println("Имя (например, \"READ\", \"WRITE\", \"DELETE\"):");
                            String permissionName = inputScanner.nextLine();
                            System.out.println("Ресурс (например, \"users\", \"reports\", \"settings\"):");
                            String resource = inputScanner.nextLine();
                            System.out.println("Описание:");
                            String description = inputScanner.nextLine();

                            try {
                                permission = new Permission(permissionName, resource, description);
                                break;
                            } catch (Exception e) {
                                System.out.println("Не удалось создать роль. Ошибка: " + e.getMessage());
                            }
                        }


                        system.roleManager.addPermissionToRole(role.getName(), permission);

                        System.out.println("Новое право успешно добавлено");
                    });

            parser.registerCommand("role-remove-permission",
                    "удалить право из роли",
                    (scanner, system) -> {
                        Scanner inputScanner = new Scanner(System.in);

                        String name = getName(inputScanner);

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
                        int permissionForRemoveNumber = Integer.parseInt(inputScanner.nextLine());
                        if (1 <= permissionForRemoveNumber && permissionForRemoveNumber <= permissions.length) {
                            int removeIdx = permissionForRemoveNumber - 1;
                            role.removePermission(permissions[removeIdx]);
                            System.out.println("Право роли успешно удалено");
                        } else {
                            System.out.println("Выбран некорректный номер");
                        }
                    });

            parser.registerCommand("role-search",
                    "поиск ролей",
                    (scanner, system) -> {
                        Scanner inputScanner = new Scanner(System.in);

                        String[] options = {"by name (contains)", "by permission", "by min count permission"};
                        int i = 1;
                        for (String option : options) {
                            System.out.println("\t" + i + ". " + option);
                            ++i;
                        }

                        int selectedFilterNumber = InputUtils.readInt(inputScanner, "Введите номер:");

                        if (selectedFilterNumber < 1 || selectedFilterNumber > options.length) {
                            throw new RuntimeException();
                        }

                        RoleFilter filter = null;
                        List<Role> roleList = null;
                        switch (selectedFilterNumber) {
                            case 1:
                                String name = InputUtils.readLine(inputScanner, "Введите имя роли или его часть:");
                                filter = RoleFilters.byNameContains(name);
                                break;
                            case 2:
                                String permissionName = InputUtils.readLine(inputScanner, "Введите название права:");
                                String resource = InputUtils.readLine(inputScanner, "Введите ресурс:");
                                roleList = system.roleManager.findRolesWithPermission(permissionName, resource);
                                break;
                            case 3:
                                int n = InputUtils.readInt(inputScanner, "Введите минимальное количество прав для роли:");
                                filter = RoleFilters.hasAtLeastNPermissions(n);
                                break;
                        }

                        if (roleList == null && filter != null) {
                            roleList = system.roleManager.findByFilter(filter);
                        }

                        if (roleList == null) {
                            throw new RuntimeException();
                        }

                        if (roleList.isEmpty()) {
                            System.out.println("Пользователей по данным фильтрам не было найдено");
                            return;
                        }
                        System.out.println("Найденные роли:");
                        int n = 1;
                        for (Role role : roleList) {
                            System.out.printf("\t%d. %s\n", n, role.format(1));
                            ++n;
                        }
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
                        List<Role> roleList = system.roleManager.findAll();

                        Optional<User> optionalUser = system.userManager.findByUsername(username);
                        if (optionalUser.isEmpty()) {
                            System.out.println("Ошибка: не удалось найти пользователя с username = " + username);
                            return;
                        }
                        User user = optionalUser.get();
                        Role selectedRole = roleList.get(selectedRoleIdx);

                        if (selectedAssignmentType == 1) {
                            newAssignment = new TemporaryAssignment(user, selectedRole, metadata, expirationDate, false);
                        } else {
                            newAssignment = new PermanentAssignment(user, selectedRole, metadata);
                        }
                        system.assignmentManager.add(newAssignment);

                        System.out.printf("Роль %s успешно назначена пользователю %s.\n", selectedRole.getName(), username);
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

                        List<RoleAssignment> roleAssignmentList = system.assignmentManager.findAll();
                        system.assignmentManager.remove(roleAssignmentList.get(selectedAssignmentIdx));

                        System.out.println("Роль успешно отозвана.");
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
                        Scanner inputScanner = new Scanner(System.in);

                        String username = InputUtils.readLine(inputScanner,
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
                        Scanner inputScanner = new Scanner(System.in);

                        String roleName = InputUtils.readLine(inputScanner,
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

                        system.assignmentManager.extendTemporaryAssignment(assignmentID, newExpirationDate);
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
                        Optional<User> optionalUser = system.userManager.findByUsername(username);
                        if (optionalUser.isEmpty()) {
                            yield null;
                        }
                        yield AssignmentFilters.byUser(optionalUser.get());
                    }
                    case 2 -> {
                        String roleName = InputUtils.readLine(inputScanner, "Роль:");
                        Optional<Role> optionalRole = system.roleManager.findByName(roleName);
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
                    results = system.assignmentManager.findByFilter(filter);
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

    private static class PermissionCommands {
        public PermissionCommands(CommandParser parser) {
            parser.registerCommand("permissions-user",
                    "все права конкретного пользователя",
                    (scanner, system) -> {
                        Scanner inputScanner = new Scanner(System.in);

                        String username = InputUtils.readLine(inputScanner, "Введите username:");

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

                        Scanner inputScanner = new Scanner(System.in);

                        String username = InputUtils.readLine(inputScanner,
                                "Введите username:");

                        String permissionName = InputUtils.readLine(inputScanner,
                                "Введите наименование права:");

                        String resource = InputUtils.readLine(inputScanner,
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
                        System.out.printf(system.generateStatistics());
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

            parser.registerCommand("audit-log",
                    "Просмотр логов",
                    (scanner, system) -> {
                        system.getAuditLog().printLog();
                    });
        }
    }
}