package org.example;

public class RBACSystem {
    UserManager userManager;
    RoleManager roleManager;
    AssignmentManager assignmentManager;
    String currentUser;

    public UserManager getUserManager() {
        return userManager;
    }

    public RoleManager getRoleManager() {
        return roleManager;
    }

    public AssignmentManager getAssignmentManager() {
        return assignmentManager;
    }

    public String getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(String currentUser) {
        this.currentUser = currentUser;
    }

    public String generateStatistics() {
        return String.format(
                        """
                        REPORT:
                            count users       : %d
                            count roles       : %d
                            count assignments : %d
                        """, userManager.count(), roleManager.count(), assignmentManager.count());
    }


    void initialize() {
        userManager = new UserManager();
        roleManager = new RoleManager();
        assignmentManager = new AssignmentManager();

        roleManager.setAssignmentManager(assignmentManager);
        assignmentManager.setRoleManager(roleManager);
        assignmentManager.setUserManager(userManager);


        Role admin = new Role("Admin", "manages everything");
        Permission adminCreateUsers = new Permission("CREATE", "users", "admin crud");
        Permission adminReadUsersData = new Permission("READ", "users data", "admin crud");
        Permission adminUpdateUsersData = new Permission("UPDATE", "users data", "admin crud");
        Permission adminDeleteUsers = new Permission("DELETE", "users", "admin crud");

        Role manager = new Role("Manager", "manages view settings");
        Permission managerUpdateSettings = new Permission("UPDATE", "settings", "manager permission");
        Permission managerReadReports = new Permission("READ", "reports", "manager permission");

        Role viewer = new Role("Viewer", "can view reports");
        Permission viewerReadReports = new Permission("READ", "reports", "viewer permission");


        User adminUser = new User("hikaruvi", "Daniil Rybkin", "i.am.daniil.rybkin@gmail.com");
        AssignmentMetadata adminMetadata =
                AssignmentMetadata.now("system", "need to add at least one admin");
        PermanentAssignment firstAdminAssignment = new PermanentAssignment(adminUser, admin, adminMetadata);

        roleManager.add(admin);
        String adminName = admin.getName();
        roleManager.addPermissionToRole(adminName, adminCreateUsers);
        roleManager.addPermissionToRole(adminName, adminUpdateUsersData);
        roleManager.addPermissionToRole(adminName, adminReadUsersData);
        roleManager.addPermissionToRole(adminName, adminDeleteUsers);

        roleManager.add(manager);
        String managerName = manager.getName();
        roleManager.addPermissionToRole(managerName, managerUpdateSettings);
        roleManager.addPermissionToRole(managerName, managerReadReports);

        roleManager.add(viewer);
        String viewerName = viewer.getName();
        roleManager.addPermissionToRole(viewerName, viewerReadReports);


        userManager.add(adminUser);
        assignmentManager.add(firstAdminAssignment);
    }
}
