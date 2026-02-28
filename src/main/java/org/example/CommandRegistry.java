package org.example;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Function;
import java.util.stream.IntStream;

public class CommandRegistry {
    private final CommandParser parser  = new CommandParser();

    public CommandRegistry() {
        new UserCommands(parser);
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
}
