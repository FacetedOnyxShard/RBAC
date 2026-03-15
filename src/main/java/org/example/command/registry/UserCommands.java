package org.example.command.registry;

import de.vandermeer.asciitable.AsciiTable;
import org.example.assignment.RoleAssignment;
import org.example.command.CommandParser;
import org.example.core.Permission;
import org.example.core.RBACSystem;
import org.example.user.User;
import org.example.user.UserFilter;
import org.example.user.UserFilters;
import org.example.util.ConsoleUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.IntStream;

import static org.example.command.registry.CommandRegistry.findUserAssignments;

public class UserCommands {
    private static boolean commandWithoutFlags(Scanner scanner) {
        return !scanner.hasNext();
    }

    private void userListLogic(Scanner scanner, RBACSystem system) {
        if (commandWithoutFlags(scanner)) {
            System.out.println("Список всех пользователей:");

            AsciiTable table = new AsciiTable();
            table.addRule();
            table.addRow("record idx", "username", "full name", "email");
            table.addRule();

            int n = 1;
            for (User user : system.getUserManager().findAll()) {
                String username = user.username();
                String fullName = user.fullName();
                String email = user.email();

                table.addRow(n, username, fullName, email);
                table.addRule();
                ++n;
            }

            System.out.println(table.render());
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
            String username = ConsoleUtils.promptString(inputScanner, UsernameFieldHelper.USERNAME.getMessage(), true);
            String fullName = ConsoleUtils.promptString(inputScanner, UsernameFieldHelper.FULL_NAME.getMessage(), true);
            String email = ConsoleUtils.promptString(inputScanner, UsernameFieldHelper.EMAIL.getMessage(), true);

            try {
                newUser = new User(username, fullName, email);
                break;
            } catch (Exception e) {
                System.out.println("Не получилось создать пользователя. Ошибка: " + e.getMessage());
            }
        }

        system.getUserManager().add(newUser);
        System.out.println("Пользователь создан успешно.");

        system.getAuditLog().log("USER_CREATE", system.getCurrentUser(), newUser.username(), "SUCCESS");
    }

    private void userViewLogic(Scanner scanner, RBACSystem system)   {
        Scanner inputScanner = new Scanner(System.in);
        String username = ConsoleUtils.promptString(inputScanner, UsernameFieldHelper.USERNAME.getMessage(), true);

        Optional<User> userOptional = system.getUserManager().findByUsername(username);

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

    private void userUpdateLogic(CommandParser parser, Scanner scanner, RBACSystem system)  {
        Scanner inputScanner = new Scanner(System.in);

        while (true) {
            parser.executeCommand("user-list", inputScanner, system);

            String username = ConsoleUtils.promptString(inputScanner,
                    UsernameFieldHelper.USERNAME.getMessage(), true);

            Optional<User> optionalUser = system.getUserManager().findByUsername(username);
            if (optionalUser.isEmpty()) {
                System.out.println("Не удалось обновить пользователя.\n" +
                        "При вводе username.\n" +
                        "Ошибка: нет пользователя с username = " + username);
                continue;
            }

            System.out.println("Введите новые данные для пользователя");
            while (true) {
                try {
                    String fullName = ConsoleUtils.promptString(inputScanner,
                            UsernameFieldHelper.FULL_NAME.getMessage(), true);
                    String email = ConsoleUtils.promptString(inputScanner,
                            UsernameFieldHelper.EMAIL.getMessage(), true);

                    system.getUserManager().update(username, fullName, email);

                    System.out.println("Данные пользователя успешно обновлены");
                    return;
                } catch (Exception e) {
                    System.out.printf("""
                            Не удалось обновить пользователя.
                            При вводе новых данных для пользователя %s.
                            Ошибка: %s
                            """, username, e.getMessage());
                    System.out.println();

                    if (!ConsoleUtils.promptYesNo(inputScanner, "Продолжить с текущим пользователем?")) {
                        break;
                    }
                }
            }
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
                (scanner, system) -> {
                    parser.executeCommand("user-list", scanner, system);
                    userViewLogic(scanner, system);
                });

        parser.registerCommand("user-update",
                "обновить данные пользователя",
                (scanner, system) -> {
                    userUpdateLogic(parser, scanner, system);
                });

        parser.registerCommand("user-delete",
                "удалить пользователя",
                (scanner, system) -> {
                    Scanner inputScanner = new Scanner(System.in);

                    parser.executeCommand("user-list", new Scanner(""), system);

                    String username = ConsoleUtils.promptString(inputScanner,
                            UsernameFieldHelper.USERNAME.getMessage(), true);

                    Optional<User> optionalUser = system.getUserManager().findByUsername(username);
                    if (optionalUser.isEmpty()) {
                        throw new RuntimeException("Пользователь не был найден");
                    }

                    if (ConsoleUtils.promptYesNo(inputScanner, "Вы уверены?")) {
                        User user = optionalUser.get();
                        List<RoleAssignment> roleAssignmentList = findUserAssignments(system, user);

                        for (RoleAssignment roleAssignment : roleAssignmentList) {
                            system.getAssignmentManager().remove(roleAssignment);
                        }
                        system.getUserManager().remove(user);

                        System.out.println("Пользователь успешно удален");
                        system.getAuditLog().log("USER_DELETE", system.getCurrentUser(), username, "SUCCESS");
                    } else {
                        System.out.println("Действие было отменено пользователем");
                    }
                });

        parser.registerCommand("user-search",
                "поиск пользователей по фильтрам",
                (scanner, system) -> {
                    Scanner inputScanner = new Scanner(System.in);

                    List<String> options = List.of(
                            "По username (содержит)",
                            "По полному имени (содержит)",
                            "По email (содержит)",
                            "По домену email");
                    int selectedFilterNumber = ConsoleUtils.promptChoice(inputScanner,
                            "Выберите фильтр (для выбора введите цифру)", options);

                    List<UsernameFieldHelper> usernameFieldHelpers = List.of(UsernameFieldHelper.fromNumber(selectedFilterNumber));
                    List<String> filterValues = new ArrayList<>();
                    filterValues.add(get(usernameFieldHelpers.getFirst(), inputScanner));

                    searchLogicForManyFilters(usernameFieldHelpers, filterValues, system);
                });
    }

    private static void searchLogicForManyFilters(List<UsernameFieldHelper> usernameFieldHelpers, List<String> filterValues, RBACSystem system) {
        List<User> userList = system.getUserManager().findAll();

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
        return ConsoleUtils.promptString(scanner, usernameFieldHelper.getMessage(), true);
    }
}