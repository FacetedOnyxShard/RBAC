package org.example.command;

import org.example.core.RBACSystem;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

public class CommandParser {
    private final Map<String, Command> commands;
    private final Map<String, String> commandDescriptions;

    public CommandParser() {
        commands = new LinkedHashMap<>();
        commandDescriptions = new LinkedHashMap<>();
    }

    public void registerCommand(String name, String description, Command command) {
        commands.put(name, command);
        commandDescriptions.put(name, description);
    }

    public void executeCommand(String commandName, Scanner scanner, RBACSystem system) {
        if (!commands.containsKey(commandName)) {
            throw new IllegalArgumentException();
        }
        commands.get(commandName).execute(scanner, system);
    }

    public void printHelp() {
        System.out.println("Command list:");
        int Id = 1;
        for (Map.Entry<String, Command> command : commands.entrySet()) {
            String commandName = command.getKey();
            System.out.println("\t" + Id + ": " + commandName + " " + commandDescriptions.get(commandName));
            Id++;
        }
    }

    public void parseAndExecute(String input, Scanner scanner, RBACSystem system) {
        String[] tokens = input.split("\\s+", 2);

        String commandName = tokens[0];
        String arguments = tokens.length > 1 ? tokens[1] : "";

        Scanner commandArgs = new Scanner(arguments);

        Command command;
        command = commands.get(commandName);
        if (command == null) {
            throw new RuntimeException("такой команды не существует");
        }

        try {
            command.execute(commandArgs, system);
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }
    }
}
