package org.example;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class CommandParser {
    private final Map<String, Command> commands;
    private final Map<String, String> commandDescriptions;

    CommandParser() {
        commands = new HashMap<>();
        commandDescriptions = new HashMap<>();
    }

    void registerCommand(String name, String description, Command command) {
        commands.put(name, command);
        commandDescriptions.put(name, description);
    }

    void executeCommand(String commandName, Scanner scanner, RBACSystem system) {
        if (!commands.containsKey(commandName)) {
            throw new IllegalArgumentException();
        }
        commands.get(commandName).execute(scanner, system);
    }

    void printHelp() {
        System.out.println("Command list:");
        int Id = 1;
        for (Map.Entry<String, Command> command : commands.entrySet()) {
            String commandName = command.getKey();
            System.out.println("\t" + Id + ": " + commandName + " " + commandDescriptions.get(commandName));
            Id++;
        }
    }

    void parseAndExecute(String input, Scanner scanner, RBACSystem system) {
        String[] parts = input.split("\\s+", 2);
        String commandName = parts[0];
        commands.get(commandName).execute(scanner, system);
    }
}
