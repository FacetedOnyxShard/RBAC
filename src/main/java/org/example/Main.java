package org.example;

import org.antlr.v4.parse.v4ParserException;
import org.example.command.CommandParser;
import org.example.command.CommandRegistry;
import org.example.role.Role;
import org.example.user.User;
import org.example.core.Permission;
import org.example.core.RBACSystem;

import java.lang.reflect.Field;
import java.text.ParseException;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        programInterface();
    }

    private static void test() {
        Scanner scanner1 = new Scanner("user-list --username hikaruvi --email yes@gmail.com");
        String command = "user-list\n";
        Scanner scanner = new Scanner(System.in);
        String input = scanner.next();
        scanner.skip("\n\r");
        System.out.println(scanner.hasNext());
    }

    private static void programInterface() {
//        Обязательные изменения
//        починить clear
//
//        Сделать нормальные временные назначения
//        Правильно форматировать дату.
//
//        Дописать stats
//        (более подробно assignments,
//        среднее кол-во ролей на пользователя,
//        топ3 популярных ролей)

//        Необязательные изменения
//          1. убрать ввод названия роли при добавлении прав только что созданной роли.
//          2. сделать чтобы роль admin нельзя было удалять и нельзя было удалить пользователя с этой ролью,
//          если он 1.
//          3. сделать нормальную обработку ошибки неверной команды



        RBACSystem system = new RBACSystem();
        system.initialize();
        system.setCurrentUser("hikaruvi");

        CommandParser parser = new CommandParser();
        new CommandRegistry(parser);

        Scanner scanner = new Scanner(System.in);
        System.out.println("""
                        ==================================
                        Информация для новых пользователей
                        ==================================
                        help - список доступных команд,
                        exit - выход
                        """);
        while (true) {
            System.out.println("Введите команду:");
            try {
                String input = scanner.nextLine();
                parser.parseAndExecute(input, scanner, system);
            } catch (Exception e) {
                System.out.println("Ошибка: " + e.getMessage());
            }

            System.out.println("\n");
        }
    }

    private static void testFromFirstTask() {
        // User
        User user1 = User.validate("hikaruvi", "Daniil Rybkin", "daniil@gmail.com");
        User user2 = User.validate("hikaruvi", "Daniil Rybkin", "daniil@gmail.com");
        User user3 = User.validate("hikaruv", "Daniil Rybkin", "daniil@gmail.com");

        System.out.println(user1.format());
        System.out.println("user1 eq user2: " + user1.equals(user2));
        System.out.println("user1 eq user3: " + user1.equals(user3));
        System.out.println();

        List<TestCase> testCases = new ArrayList<>();

        testCases.add(new TestCase(null, "Daniil Rybkin", "d@gmail.com", false));
        testCases.add(new TestCase("hikaruvi", null, "d@gmail.com", false));
        testCases.add(new TestCase("hikaruvi", "Dan Ryb", null, false));

        testCases.add(new TestCase("   \n  \t \r \f ", "Dan Ryb", "d@gmail.com", false));
        testCases.add(new TestCase("hikaruvi", "   \n  \t \r \f ", "d@gmail.com", false));
        testCases.add(new TestCase("hikaruvi", "Dan Ryb", "   \n  \t \r \f ", false));

        testCases.add(new TestCase("hikaruv$", "Dan Ry", "d@mail.com", false));
        testCases.add(new TestCase("hikaruvi", "Dan Ry", "d@mail.com", true));

        testCases.add(new TestCase("hi", "Dan Ry", "d@mail.com", false));
        testCases.add(new TestCase("hik", "Dan Ry", "d@mail.com", true));
        testCases.add(new TestCase("hihihihihihihihihihik", "Dan Ry", "d@mail.com", false));
        testCases.add(new TestCase("hihihihihihihihihihi", "Dan Ry", "d@mail.com", true));

        testCases.add(new TestCase("hihihihihihihihihi", "Dan Ry", "dmail.com", false));
        testCases.add(new TestCase("hihihihihihihihihi", "Dan Ry", "d@mailcom", false));

        runTests(testCases);

        // Role
        Permission p1 = new Permission("READ", "users", "undefined");
        Permission p2 = new Permission("WRITE", "users", "undefined");

        Set<Permission> ps = Set.of(p1, p2);

        Role r = new Role("ADM", "super man", ps);
        System.out.println(r.format());
    }

    public record TestCase(String username, String fullName, String email, boolean shouldPass) {
        @Override
        public String toString() {
            return String.format("input values: %s, %s, %s; should pass: %b", username, fullName, email, shouldPass);
        }
    }

    static void runTests(List<TestCase> testCases, boolean verbose) {
        int passed = 0;
        int failed = 0;

        for (int i = 0; i < testCases.size(); ++i) {
            TestCase test = testCases.get(i);
            if (verbose) {
                System.out.printf("Test #%d: %s\n", i + 1, test.toString());
            }

            try {
                User user = User.validate(test.username(), test.fullName(), test.email());

                if (test.shouldPass()) {
                    if (verbose) System.out.println("  Success: Created user: " + user.format());
                    passed++;
                } else {
                    if (verbose) System.out.println("  Fail: Should be fail, but user created");
                    failed++;
                }
            } catch (IllegalArgumentException e) {
                if (test.shouldPass()) {
                    if (verbose) System.out.println("  Fail: Should pass, but fail: " + e.getMessage());
                    failed++;
                } else {
                    if (verbose) System.out.println("  Success: Success failed: " + e.getMessage());
                    passed++;
                }
            }

            if (verbose) System.out.println();
        }

        System.out.println("=== Test results ===");
        System.out.println("Passed: " + passed);
        System.out.println("Failed: " + failed);
        System.out.println("All: " + testCases.size());
    }

    static void runTests(List<TestCase> testCases) {
        runTests(testCases, false);
    }
}