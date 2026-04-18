package org.example.util;

import java.util.List;
import java.util.Scanner;

public class ConsoleUtils {

    public static String promptString(Scanner scanner, String message, boolean required) {
        while (true) {
            System.out.println(message);

            String input = ValidationUtils.normalizeString(scanner.nextLine());

            if (!input.isBlank()) {
                return input;
            }

            if (!required) {
                return "";
            }

            System.out.println("Нужно ввести обязательно, ввод должен быть не пустым");
        }
    }

    public static int promptInt(Scanner scanner, String message, int min, int max) {
        System.out.println(message);
        int n = Integer.parseInt(scanner.nextLine());
        if (!(n >= min && n <= max)) {
            throw new RuntimeException("Значение лежит в недопустимом диапазоне");
        }
        return n;
    }

    public static boolean promptYesNo(Scanner scanner, String message) {
        System.out.println(message + " (yes/no)");
        return scanner.nextLine().equalsIgnoreCase("yes");
    }

    public static int promptChoice(Scanner scanner, String message, List<String> options) {
        if (options == null || options.isEmpty()) {
            throw new IllegalArgumentException("Список опций не может быть пустым");
        }

        System.out.println(message);
        int n = 1;
        for (String option : options) {
            System.out.printf("\t%d. %s\n", n, option);
            ++n;
        }

        String selectMessage = String.format("Введите число от 1 до %d", options.size());
        while (true) {
            try {
                return promptInt(scanner, selectMessage, 1, options.size());
            } catch (Exception e) {
                System.out.println("Число некорректно\n");
            }
        }
    }
}