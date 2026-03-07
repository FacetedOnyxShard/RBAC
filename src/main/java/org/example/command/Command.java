package org.example.command;

import org.example.core.RBACSystem;

import java.util.Scanner;

@FunctionalInterface
interface Command {
    void execute(Scanner scanner, RBACSystem system);
}

