package org.example.util;

import java.util.Scanner;

public class InputUtils {
    public static String readLine(Scanner scanner, String prompt) {
        System.out.println(prompt);
        return scanner.nextLine();
    }

    public static String readDateInString(Scanner scanner, String prompt) {
        System.out.println(prompt);
        return scanner.nextLine();
    }

    public static int readInt(Scanner scanner, String prompt) {
        System.out.println(prompt);
        return Integer.parseInt(scanner.nextLine());
    }

    public static boolean confirm(Scanner scanner, String prompt) {
        System.out.println(prompt + " (да/нет)");
        return scanner.nextLine().trim().equalsIgnoreCase("да");
    }
}