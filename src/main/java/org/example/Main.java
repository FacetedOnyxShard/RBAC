package org.example;

import java.util.*;

public class Main {
    public static void main(String[] args) {
        testCommands();
    }

    private static void testCommands() {
        int i = 1;
        String str = "hello";
        System.out.println("Список:");
        System.out.printf("\t%d. %s", i, str);
    }

    private static void testScanner() {
        Scanner scanner = new Scanner("print Hello, Wolrd!");
        while (scanner.hasNext()) {
            System.out.println(scanner.next());
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