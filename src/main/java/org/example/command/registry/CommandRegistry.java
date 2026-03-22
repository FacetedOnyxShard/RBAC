package org.example.command.registry;

import org.example.assignment.*;
import org.example.command.CommandParser;
import org.example.core.RBACSystem;
import org.example.role.Role;
import org.example.user.User;
import org.example.util.ReportGenerator;

import java.util.*;
import java.util.stream.Collectors;

public class CommandRegistry {
    public CommandRegistry(CommandParser parser) {
        new UserCommands(parser);
        new RoleCommands(parser);
        new AssignmentCommands(parser);
        new PermissionCommands(parser);
        new UtilityCommands(parser);
    }

    public static List<RoleAssignment> findUserAssignments(RBACSystem system, User user) {
        AssignmentFilter filterAssignmentByUser = AssignmentFilters.byUser(user);
        return system.getAssignmentManager().findByFilter(filterAssignmentByUser);
    }

    private static class UtilityCommands {
        public UtilityCommands(CommandParser parser) {
            parser.registerCommand("help",
                    "справка по командам",
                    (scanner, system) -> {
                        parser.printHelp();
                    });

            parser.registerCommand("stats",
                    "статистика системы",
                    (scanner, system) -> {
                        System.out.printf(system.generateStatistics());
                        List<String> statsMetric = List.of(
                                "Количество назначений активных",
                                "Количество назначений истекших",
                                "Среднее количество ролей на пользователя",
                                "Топ-3 самых популярных ролей"
                        );

                        List<String> metricValues = new ArrayList<>();
                        metricValues.add(Integer.toString(system.getAssignmentManager().getActiveAssignments().size()));
                        metricValues.add(Integer.toString(system.getAssignmentManager().getExpiredAssignments().size()));
                        metricValues.add(Float.toString(roundFloat(system.getUserManager().count() / (float)system.getRoleManager().count(), 2)));

                        Map<Role, Long> roleCount = system.getAssignmentManager().findAll().stream()
                                .collect(Collectors.groupingBy(
                                        RoleAssignment::role,
                                        Collectors.counting()
                                ));


                        int[] counter = {0};
                        String mostPopularRoles = roleCount.entrySet().stream()
                                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                                .limit(3)
                                .map(entry -> {
                                    return String.format(
                                            "\t\t%d. %s: %d assignment(s)",
                                            ++counter[0],
                                            entry.getKey().getName(),
                                            entry.getValue()
                                    );
                                }).collect(Collectors.joining("\n"));

                        metricValues.add(mostPopularRoles);


                        for (int i = 0; i < statsMetric.size(); ++i) {
                            System.out.printf("\t%s: ", statsMetric.get(i));
                            if (i == statsMetric.size() - 1) System.out.println();
                            System.out.printf("%s\n", metricValues.get(i));
                        }
                    });

            parser.registerCommand("clear",
                    "очистить экран",
                    (scanner, system) -> {
                        for (int i = 0; i < 50; ++i) {
                            System.out.println();
                        }
                    });

            parser.registerCommand("exit",
                    "выход из программы",
                    (scanner, system) -> {
                        scanner.close();
                        System.out.println("Остановка выполнения программы...");
                        System.exit(0);
                    });

            parser.registerCommand("audit-log",
                    "Просмотр логов",
                    (scanner, system) -> {
                        system.getAuditLog().printLog();
                    });

            parser.registerCommand("audit-log-file",
                    "Просмотр логов в файле",
                    (scanner, system) -> {
                        system.getAuditLog().saveToFile("rbac-reports/log.txt");
                    });

            parser.registerCommand("report-users", "вывести/сохранить отчёт по пользователям",
                    (scanner, system) -> {
                        ReportGenerator reportGenerator = new ReportGenerator();

                        String filepath = "rbac-reports/users.txt";

                        try {
                            String report = reportGenerator.generateUserReport(
                                    system.getUserManager(),
                                    system.getAssignmentManager()
                            );

                            reportGenerator.exportToFile(report, filepath);
                        } catch (Exception e) {
                            System.out.printf("Error generating user report: %s\n", e.getMessage());
                        }
                    });

            parser.registerCommand("report-roles", "отчёт по ролям",
                    (scanner, system) -> {
                        ReportGenerator reportGenerator = new ReportGenerator();

                        String filepath = "rbac-reports/roles.txt";

                        try {
                            String report = reportGenerator.generateRoleReport(
                                    system.getRoleManager(),
                                    system.getAssignmentManager()
                            );

                            reportGenerator.exportToFile(report, filepath);

                        } catch (Exception e) {
                            System.out.printf("Error generating role report: %s\n", e.getMessage());
                        }
                    });

            parser.registerCommand("report-matrix", "Generate permission matrix (users × resources)",
                    (scanner, system) -> {
                        ReportGenerator reportGenerator = new ReportGenerator();

                        String filepath = "rbac-reports/permission_matrix.txt";

                        try {
                            String report = reportGenerator.generatePermissionMatrix(
                                    system.getUserManager(),
                                    system.getAssignmentManager()
                            );

                            reportGenerator.exportToFile(report, filepath);

                        } catch (Exception e) {
                            System.out.printf("Error generating permission matrix: %s\n", e.getMessage());
                        }
                    });
        }
    }

    public static float roundFloat(float value, int places) {
        float scale = (float) Math.pow(10, places);
        return Math.round(value * scale) / scale;
    }
}