package org.example.command.registry;

import de.vandermeer.asciitable.AsciiTable;
import org.example.assignment.RoleAssignment;
import org.example.command.CommandParser;
import org.example.core.Permission;
import org.example.role.Role;
import org.example.role.RoleFilter;
import org.example.role.RoleFilters;
import org.example.user.User;
import org.example.util.InputUtils;

import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class RoleCommands {
    public RoleCommands(CommandParser parser) {
        parser.registerCommand("role-list",
                "вывести список всех ролей",
                (scanner, system) -> {
                    System.out.println("Список ролей:");

                    AsciiTable table = new AsciiTable();
                    table.addRule();
                    table.addRow("Номер", "Название", "Количество прав", "ID");
                    table.addRule();

                    int n = 1;
                    for (Role role : system.getRoleManager().findAll()) {
                        String roleName = role.getName();
                        String permissionCount = Integer.toString(role.getPermissions().size());
                        String id = role.getId();

                        table.addRow(n, roleName, permissionCount, id);
                        table.addRule();
                        ++n;
                    }

                    System.out.println(table.render());
                });

        parser.registerCommand("role-create",
                "создать новую роль",
                (scanner, system) -> {
                    Scanner inputScanner = new Scanner(System.in);

                    Role newRole;
                    while (true) {
                        String name = getName(inputScanner);
                        String description = getDescription(inputScanner);

                        try {
                            newRole = new Role(name, description);
                            system.getRoleManager().add(newRole);

                            while (InputUtils.confirm(inputScanner, "Хотите добавить новое право")) {
                                parser.executeCommand("role-add-permission", inputScanner, system);
                            }
                            break;
                        } catch (Exception e) {
                            System.out.println("Не удалось создать роль. Ошибка " + e.getMessage());
                        }
                    }
                    System.out.println("Новая роль успешно создана");
                    system.getAuditLog().log("ROLE_CREATE", system.getCurrentUser(), newRole.getName(), "SUCCESS");
                });

        parser.registerCommand("role-view",
                "просмотр роли",
                (scanner, system) -> {
                    Scanner inputScanner = new Scanner(System.in);

                    String name = getName(inputScanner);

                    Optional<Role> optionalRole = system.getRoleManager().findByName(name);
                    if (optionalRole.isEmpty()) {
                        System.out.println("Роль с именем " + name + " не была найдена");
                        return;
                    }

                    Role role = optionalRole.get();
                    System.out.println(role.format());
                });

        parser.registerCommand("role-update",
                "обновить роль (название/описание)",
                (scanner, system) -> {
                    Scanner inputScanner = new Scanner(System.in);

                    String name = getName(inputScanner);
                    String description = getDescription(inputScanner);

                    Optional<Role> optionalRole = system.getRoleManager().findByName(name);
                    if (optionalRole.isEmpty()) {
                        System.out.println("Роль с именем " + name + " не была найдена");
                        return;
                    }

                    String newName = getName(inputScanner);

                    Role role = optionalRole.get();
                    role.setName(newName);
                    role.setDescription(description);
                });

        parser.registerCommand("role-delete",
                "удалить роль",
                (scanner, system) -> {
                    Scanner inputScanner = new Scanner(System.in);

                    String name = getName(inputScanner);

                    Optional<Role> optionalRole = system.getRoleManager().findByName(name);
                    if (optionalRole.isEmpty()) {
                        System.out.println("Роль с именем " + name + " не была найдена");
                        return;
                    }

                    Role role = optionalRole.get();

                    List<RoleAssignment> roleAssignmentList = system.getAssignmentManager().findByRole(role);

                    if (!roleAssignmentList.isEmpty()) {
                        System.out.println("Не удалось удалить роль. Причина: роль назначена пользователям");
                        System.out.println("Список пользователей, которым назначена роль:");
                        int i = 1;
                        for (RoleAssignment roleAssignment : roleAssignmentList) {
                            System.out.println("\t" + i + ". " + roleAssignment.user());
                            ++i;
                        }
                        return;
                    }

                    if (prompt(inputScanner)) {
                        system.getRoleManager().remove(role);
                        System.out.println("Роль успешно удалена");
                        system.getAuditLog().log("ROLE_DELETE", system.getCurrentUser(), name, "SUCCESS");
                    } else {
                        System.out.println("Отмена удаления роли...");
                    }
                });

        parser.registerCommand("role-add-permission",
                "добавить право к роли",
                (scanner, system) -> {
                    Scanner inputScanner = new Scanner(System.in);

                    String name = getName(inputScanner);

                    Optional<Role> optionalRole = system.getRoleManager().findByName(name);
                    if (optionalRole.isEmpty()) {
                        System.out.println("Роль с именем " + name + " не была найдена");
                        return;
                    }

                    Role role = optionalRole.get();
                    Permission permission;

                    while (true) {
                        System.out.println("Введите данные права");
                        System.out.println("Имя (например, \"READ\", \"WRITE\", \"DELETE\"):");
                        String permissionName = inputScanner.nextLine();
                        System.out.println("Ресурс (например, \"users\", \"reports\", \"settings\"):");
                        String resource = inputScanner.nextLine();
                        System.out.println("Описание:");
                        String description = inputScanner.nextLine();

                        try {
                            permission = new Permission(permissionName, resource, description);
                            break;
                        } catch (Exception e) {
                            System.out.println("Не удалось создать роль. Ошибка: " + e.getMessage());
                        }
                    }


                    system.getRoleManager().addPermissionToRole(role.getName(), permission);

                    System.out.println("Новое право успешно добавлено");
                });

        parser.registerCommand("role-remove-permission",
                "удалить право из роли",
                (scanner, system) -> {
                    Scanner inputScanner = new Scanner(System.in);

                    String name = getName(inputScanner);

                    Optional<Role> optionalRole = system.getRoleManager().findByName(name);
                    if (optionalRole.isEmpty()) {
                        System.out.println("Роль с именем " + name + " не была найдена");
                        return;
                    }

                    Role role = optionalRole.get();
                    Permission[] permissions = role.getPermissions().toArray(new Permission[0]);
                    int i = 1;
                    System.out.println("Права роли:");
                    for (Permission permission : permissions) {
                        System.out.printf("\t%d. %s\n", i, permission.format());
                        ++i;
                    }

                    System.out.println("Введите номер права для удаления:");
                    int permissionForRemoveNumber = Integer.parseInt(inputScanner.nextLine());
                    if (1 <= permissionForRemoveNumber && permissionForRemoveNumber <= permissions.length) {
                        int removeIdx = permissionForRemoveNumber - 1;
                        role.removePermission(permissions[removeIdx]);
                        System.out.println("Право роли успешно удалено");
                    } else {
                        System.out.println("Выбран некорректный номер");
                    }
                });

        parser.registerCommand("role-search",
                "поиск ролей",
                (scanner, system) -> {
                    Scanner inputScanner = new Scanner(System.in);

                    String[] options = {"by name (contains)", "by permission", "by min count permission"};
                    int i = 1;
                    for (String option : options) {
                        System.out.println("\t" + i + ". " + option);
                        ++i;
                    }

                    int selectedFilterNumber = InputUtils.readInt(inputScanner, "Введите номер:");

                    if (selectedFilterNumber < 1 || selectedFilterNumber > options.length) {
                        throw new RuntimeException();
                    }

                    RoleFilter filter = null;
                    List<Role> roleList = null;
                    switch (selectedFilterNumber) {
                        case 1:
                            String name = InputUtils.readLine(inputScanner, "Введите имя роли или его часть:");
                            filter = RoleFilters.byNameContains(name);
                            break;
                        case 2:
                            String permissionName = InputUtils.readLine(inputScanner, "Введите название права:");
                            String resource = InputUtils.readLine(inputScanner, "Введите ресурс:");
                            roleList = system.getRoleManager().findRolesWithPermission(permissionName, resource);
                            break;
                        case 3:
                            int n = InputUtils.readInt(inputScanner, "Введите минимальное количество прав для роли:");
                            filter = RoleFilters.hasAtLeastNPermissions(n);
                            break;
                    }

                    if (roleList == null && filter != null) {
                        roleList = system.getRoleManager().findByFilter(filter);
                    }

                    if (roleList == null) {
                        throw new RuntimeException();
                    }

                    if (roleList.isEmpty()) {
                        System.out.println("Пользователей по данным фильтрам не было найдено");
                        return;
                    }
                    System.out.println("Найденные роли:");
                    int n = 1;
                    for (Role role : roleList) {
                        System.out.printf("\t%d. %s\n", n, role.format(1));
                        ++n;
                    }
                });
    }

    private static boolean prompt(Scanner scanner) {
        System.out.println("Вы уверены да/нет?");
        String answer = scanner.nextLine().trim();
        return answer.equals("да");
    }

    private static String getName(Scanner scanner) {
        System.out.println("Введите название роли: ");
        return scanner.nextLine();
    }

    private static String getDescription(Scanner scanner) {
        System.out.println("Введите описание роли:");
        return scanner.nextLine();
    }
}