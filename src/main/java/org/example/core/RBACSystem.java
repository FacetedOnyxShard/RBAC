package org.example.core;

import org.example.assignment.*;
import org.example.role.Role;
import org.example.role.RoleManager;
import org.example.user.User;
import org.example.user.UserManager;
import org.example.util.AuditLog;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RBACSystem {
    private UserManager userManager;
    private RoleManager roleManager;
    private AssignmentManager assignmentManager;
    private AuditLog auditLog;
    private String currentUser;

    private ExecutorService executorService;

    private ScheduledExecutorService scheduledExecutorService;
    private volatile boolean scheduledTaskRunning = true;

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public UserManager getUserManager() {
        return userManager;
    }

    public RoleManager getRoleManager() {
        return roleManager;
    }

    public AssignmentManager getAssignmentManager() {
        return assignmentManager;
    }

    public AuditLog getAuditLog() {
        return auditLog;
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
                        \tcount users       : %d
                        \tcount roles       : %d
                        \tcount assignments : %d
                        """, userManager.count(), roleManager.count(), assignmentManager.count());
    }


    public void initialize() {
        userManager = new UserManager();
        roleManager = new RoleManager();
        assignmentManager = new AssignmentManager();
        auditLog = new AuditLog();

        executorService = Executors.newFixedThreadPool(4);

        roleManager.setAssignmentManager(assignmentManager);
        assignmentManager.setRoleManager(roleManager);
        assignmentManager.setUserManager(userManager);

        createInitialRBACStructure();

        startScheduledTasks();
    }

    private void createInitialRBACStructure() {
        Role admin = new Role("Admin", "manages everything");
        Permission adminCreateUsers = new Permission("CREATE", "users", "admin crud");
        Permission adminReadUsersData = new Permission("READ", "users data", "admin crud");
        Permission adminUpdateUsersData = new Permission("UPDATE", "users data", "admin crud");
        Permission adminDeleteUsers = new Permission("DELETE", "users", "admin crud");
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
        userManager.add(adminUser);
        assignmentManager.add(firstAdminAssignment);



        Role manager = new Role("Manager", "manages view settings");
        Permission managerUpdateSettings = new Permission("UPDATE", "settings", "manager permission");
        Permission managerReadReports = new Permission("READ", "reports", "manager permission");

        roleManager.add(manager);
        String managerName = manager.getName();
        roleManager.addPermissionToRole(managerName, managerUpdateSettings);
        roleManager.addPermissionToRole(managerName, managerReadReports);



        Role viewer = new Role("Viewer", "can view reports");
        Permission viewerReadReports = new Permission("READ", "reports", "viewer permission");

        roleManager.add(viewer);
        String viewerName = viewer.getName();
        roleManager.addPermissionToRole(viewerName, viewerReadReports);



        Role creator = new Role("Creator", "create admins");
        Permission creatorCreateAdmins = new Permission("CREATE", "admins", "no description");
        User creatorUser = new User("jon_doe", "Jon Doe", "jon.doe@yandex.ru");

        AssignmentMetadata creatorMetadata =
                AssignmentMetadata.now("system", "no reason");
        PermanentAssignment creatorAssignment = new PermanentAssignment(creatorUser, creator, creatorMetadata);

        roleManager.add(creator);
        String creatorName = creator.getName();
        roleManager.addPermissionToRole(creatorName, creatorCreateAdmins);
        userManager.add(creatorUser);
        assignmentManager.add(creatorAssignment);



//        1. Менеджер контента - может управлять статьями и медиафайлами
        Role contentManager = new Role("ContentManager", "manage content and media");
        Permission contentManagerCreateArticles = new Permission("CREATE", "articles", "can create new articles");
        Permission contentManagerEditArticles = new Permission("EDIT", "articles", "can edit existing articles");
        Permission contentManagerUploadMedia = new Permission("UPLOAD", "media", "can upload images and videos");
        User contentUser = new User("anna_smith", "Anna Smith", "anna.smith@company.com");

        AssignmentMetadata contentMetadata = AssignmentMetadata.now("hr_department", "content team lead");
        PermanentAssignment contentAssignment = new PermanentAssignment(contentUser, contentManager, contentMetadata);

        roleManager.add(contentManager);
        roleManager.addPermissionToRole(contentManager.getName(), contentManagerCreateArticles);
        roleManager.addPermissionToRole(contentManager.getName(), contentManagerEditArticles);
        roleManager.addPermissionToRole(contentManager.getName(), contentManagerUploadMedia);
        userManager.add(contentUser);
        assignmentManager.add(contentAssignment);

//        2. Аналитик данных - может просматривать отчеты и экспортировать данные
        Role dataAnalyst = new Role("DataAnalyst", "view reports and export data");
        Permission analystViewReports = new Permission("VIEW", "reports", "can view all reports");
        Permission analystExportData = new Permission("EXPORT", "data", "can export data to CSV/Excel");
        Permission analystCreateDashboards = new Permission("CREATE", "dashboards", "can create custom dashboards");
        User analystUser = new User("mike_johnson", "Mike Johnson", "mike.johnson@analytics.com");

        AssignmentMetadata analystMetadata = AssignmentMetadata.now("analytics_director", "hired for Q3 reporting");
        PermanentAssignment analystAssignment = new PermanentAssignment(analystUser, dataAnalyst, analystMetadata);

        roleManager.add(dataAnalyst);
        roleManager.addPermissionToRole(dataAnalyst.getName(), analystViewReports);
        roleManager.addPermissionToRole(dataAnalyst.getName(), analystExportData);
        roleManager.addPermissionToRole(dataAnalyst.getName(), analystCreateDashboards);
        userManager.add(analystUser);
        assignmentManager.add(analystAssignment);

//        3. Модератор - может блокировать пользователей и удалять спам
        Role moderator = new Role("Moderator", "block users and remove spam");
        Permission moderatorBlockUsers = new Permission("BLOCK", "users", "can block suspicious users");
        Permission moderatorDeleteContent = new Permission("DELETE", "content", "can delete inappropriate content");
        Permission moderatorViewReports = new Permission("VIEW", "user_reports", "can view user complaints");
        User moderatorUser = new User("elena_wilson", "Elena Wilson", "elena.w@moderation.com");

        AssignmentMetadata moderatorMetadata = AssignmentMetadata.now("security_team", "night shift moderator");
        TemporaryAssignment moderatorAssignment = new TemporaryAssignment(moderatorUser, moderator, moderatorMetadata, "2026-03-09 00:00:00", false);

        roleManager.add(moderator);
        roleManager.addPermissionToRole(moderator.getName(), moderatorBlockUsers);
        roleManager.addPermissionToRole(moderator.getName(), moderatorDeleteContent);
        roleManager.addPermissionToRole(moderator.getName(), moderatorViewReports);
        userManager.add(moderatorUser);
        assignmentManager.add(moderatorAssignment);

//        4. Разработчик - доступ к коду, логам и деплою
        Role developer = new Role("Developer", "access to code, logs and deployment");
        Permission developerViewCode = new Permission("VIEW", "repository", "can view source code");
        Permission developerDeploy = new Permission("DEPLOY", "application", "can deploy to staging");
        Permission developerViewLogs = new Permission("VIEW", "logs", "can view system logs");
        User devUser = new User("alex_chen", "Alex Chen", "alex.chen@dev.team");

        AssignmentMetadata devMetadata = AssignmentMetadata.now("tech_lead", "senior backend developer");
        PermanentAssignment devAssignment = new PermanentAssignment(devUser, developer, devMetadata);

        roleManager.add(developer);
        roleManager.addPermissionToRole(developer.getName(), developerViewCode);
        roleManager.addPermissionToRole(developer.getName(), developerDeploy);
        roleManager.addPermissionToRole(developer.getName(), developerViewLogs);
        userManager.add(devUser);
        assignmentManager.add(devAssignment);

//        5. Бухгалтер - работа с финансами и зарплатами
        Role accountant = new Role("Accountant", "manage finances and payroll");
        Permission accountantViewFinance = new Permission("VIEW", "finance", "can view financial data");
        Permission accountantProcessPayroll = new Permission("PROCESS", "payroll", "can process monthly payroll");
        Permission accountantGenerateReports = new Permission("GENERATE", "financial_reports", "can generate tax reports");
        User accountantUser = new User("olga_petrova", "Olga Petrova", "olga.p@finance.com");

        AssignmentMetadata accountantMetadata = AssignmentMetadata.now("finance_director", "hired for 2024");
        PermanentAssignment accountantAssignment = new PermanentAssignment(accountantUser, accountant, accountantMetadata);

        roleManager.add(accountant);
        roleManager.addPermissionToRole(accountant.getName(), accountantViewFinance);
        roleManager.addPermissionToRole(accountant.getName(), accountantProcessPayroll);
        roleManager.addPermissionToRole(accountant.getName(), accountantGenerateReports);
        userManager.add(accountantUser);
        assignmentManager.add(accountantAssignment);
    }

    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        if (scheduledExecutorService != null && !scheduledExecutorService.isShutdown()) {
            scheduledTaskRunning = false;
            scheduledExecutorService.shutdown();
            try {
                if (!scheduledExecutorService.awaitTermination(10, TimeUnit.SECONDS)) {
                    scheduledExecutorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduledExecutorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        if (auditLog != null) {
            auditLog.shutdown();
        }
    }

    private void startScheduledTasks() {
        scheduledExecutorService = Executors.newScheduledThreadPool(2);

        scheduledExecutorService.scheduleAtFixedRate(() -> {
            try {
                expireTemporaryAssignments();
            } catch (Exception e) {
                System.err.println("[ScheduledTask] Error expiring assignments: " + e.getMessage());
            }
        }, 10, 30, TimeUnit.SECONDS);

        scheduledExecutorService.scheduleAtFixedRate(() -> {
            try {
                logStatistics();
            } catch (Exception e) {
                System.err.println("[ScheduledTask] Error logging statistics: " + e.getMessage());
            }
        }, 15, 60, TimeUnit.SECONDS);
    }

    private void expireTemporaryAssignments() {
        List<RoleAssignment> allAssignments = assignmentManager.findAll();
        List<RoleAssignment> expiredTemporary = allAssignments.stream()
                .filter(assignment -> assignment instanceof TemporaryAssignment)
                .filter(assignment -> !assignment.isActive())
                .toList();

        if (!expiredTemporary.isEmpty()) {
            for (RoleAssignment assignment : expiredTemporary) {
                if (assignment instanceof TemporaryAssignment temp) {
                    if (auditLog != null) {
                        auditLog.log("expire-check", "scheduler",
                                assignment.user().username(),
                                "Temporary assignment expired for role: " + assignment.role().getName());
                    }
                }
            }
        }
    }

    private void logStatistics() {
        String stats = generateStatistics();
        if (auditLog != null) {
            String[] lines = stats.split("\n");
            for (String line : lines) {
                auditLog.log("statistics", "scheduler", "system", line);
            }
        }
    }
}
