package org.example.command.registry;

import org.example.assignment.RoleAssignment;
import org.example.command.CommandParser;
import org.example.core.Permission;
import org.example.core.RBACSystem;
import org.example.user.User;
import org.example.user.UserFilter;
import org.example.user.UserFilters;
import org.example.util.InputUtils;

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
            int n = 1;
            for (User user : system.getUserManager().findAll()) {
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

        system.getUserManager().add(newUser);
        System.out.println("Пользователь создан успешно.");

        system.getAuditLog().log("USER_CREATE", system.getCurrentUser(), newUser.username(), "SUCCESS");
    }

    private void userViewLogic(Scanner scanner, RBACSystem system)   {
        Scanner inputScanner = new Scanner(System.in);
        String username = InputUtils.readLine(inputScanner, UsernameFieldHelper.USERNAME.getMessage());

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
                    Scanner inputScanner = new Scanner(System.in);
                    while (true) {
                        try {
                            String username = InputUtils.readLine(inputScanner, UsernameFieldHelper.USERNAME.getMessage());
                            System.out.println("Новые данные пользователя");
                            String fullName = InputUtils.readLine(inputScanner, UsernameFieldHelper.FULL_NAME.getMessage());
                            String email = InputUtils.readLine(inputScanner, UsernameFieldHelper.EMAIL.getMessage());
                            system.getUserManager().update(username, fullName, email);
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
                        Optional<User> optionalUser = system.getUserManager().findByUsername(username);

                        if (optionalUser.isEmpty()) {
                            System.out.println("Пользователь не был найден");
                            return;
                        }

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

        List<User> userList = system.getUserManager().findByFilter(filter);

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