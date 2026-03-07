package org.example;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.junitpioneer.jupiter.*;
import org.junitpioneer.jupiter.cartesian.CartesianTest;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class TestManagers {
    @Nested
    public class TestUser {
        private UserManager userManager;

        @BeforeEach
        void beforeE() {
            userManager = new UserManager();
        }

        @AfterEach
        void afterE() {
            userManager.clear();
        }

        @Test
        public void add_default() {
            User user = new User("hikaruvi", "daniil rybkin", "dan@gmail.com");

            userManager.add(user);

            assertEquals(1, userManager.count());
            assertTrue(userManager.findAll().contains(user));
        }

        @Test
        public void remove_default() {
            User user = new User("hikaruvi", "daniil rybkin", "dan@gmail.com");

            userManager.add(user);
            userManager.remove(user);

            assertEquals(0, userManager.count());
            assertFalse(userManager.findAll().contains(user));
        }

        @Test
        public void findById_default() {
            User expectedUser = generateUser();
            userManager.add(generateUser());
            userManager.add(expectedUser);
            userManager.add(generateUser());

            Optional<User> optionalFoundUser = userManager.findById(expectedUser.username());

            if (optionalFoundUser.isPresent()) {
                User foundUser = optionalFoundUser.get();
                assertEquals(expectedUser, foundUser);
            } else {
                fail("User должен быть найден");
            }
        }

        @Test
        public void findByUsername_default() {
            User expectedUser = generateUser();
            userManager.add(generateUser());
            userManager.add(expectedUser);
            userManager.add(generateUser());

            Optional<User> optionalFoundUser = userManager.findByUsername(expectedUser.username());

            if (optionalFoundUser.isPresent()) {
                User foundUser = optionalFoundUser.get();
                assertEquals(expectedUser, foundUser);
            } else {
                fail("User должен быть найден");
            }
        }

        @Test
        public void findByEmail_default() {
            User expectedUser = generateUser();
            userManager.add(generateUser());
            userManager.add(expectedUser);
            userManager.add(generateUser());

            Optional<User> optionalFoundUser = userManager.findByEmail(expectedUser.email());

            if (optionalFoundUser.isPresent()) {
                User foundUser = optionalFoundUser.get();
                assertEquals(expectedUser, foundUser);
            } else {
                fail("User должен быть найден");
            }
        }

        private UserFilter createFilterWithParameter(String filterName, String parameter) {
            return switch (filterName) {
                case "byEmail" -> UserFilters.byEmail(parameter);
                case "byEmailDomain" -> UserFilters.byEmailDomain(parameter);
                case "byFullNameContains" -> UserFilters.byFullNameContains(parameter);
                case "byUsername" -> UserFilters.byUsername(parameter);
                case "byUsernameContains" -> UserFilters.byUsernameContains(parameter);
                default -> throw new IllegalArgumentException("Unknown filter: " + filterName);
            };
        }

        @ParameterizedTest
        @CsvSource({
                "byEmail, dannil.rybkin@gmail.com",
                "byEmailDomain, gmail.com",
                "byFullNameContains, Rybkin",
                "byUsername, hikaruvi",
                "byUsernameContains, ar",
        })
        public void findByFilter_default(String filterName, String filterParameter) {
            UserFilter filter = createFilterWithParameter(filterName, filterParameter);
            List<User> expectedUsers = new ArrayList<>();
            for (int i = 0; i < 10; ++i) {
                User newUser = generateUser();
                userManager.add(newUser);
                if (filter.test(newUser)) {
                    expectedUsers.add(newUser);
                }
            }

            List<User> foundUsers = userManager.findByFilter(filter);

            assertEquals(new HashSet<>(expectedUsers), new HashSet<>(foundUsers));
        }


        @Test
        public void findAll_noParams_default() {
            final int expectedSize = 10;
            Set<User> expectedValues = new HashSet<>();
            for (int i = 0; i < expectedSize; ++i) {
                User newUser = generateUser();
                expectedValues.add(newUser);
                userManager.add(newUser);
            }

            List<User> methodResult = userManager.findAll();

            assertEquals(expectedSize, userManager.count());
            assertEquals(expectedValues, new HashSet<>(methodResult));
        }

        @Test
        public void count_default() {
            final int expectedSize = random.nextInt(1, 1000);
            for (int i = 0; i < expectedSize; ++i) {
                User newUser = generateUser();
                userManager.add(newUser);
            }

            final int countUsers = userManager.count();

            assertEquals(expectedSize, countUsers);
        }

        @Test
        public void clear_default() {
            final int expectedSize = random.nextInt(1, 1000);
            for (int i = 0; i < expectedSize; ++i) {
                User newUser = generateUser();
                userManager.add(newUser);
            }

            final int countUsersBeforeClear = userManager.count();
            userManager.clear();
            final int countUsersAfterClear = userManager.count();

            assertTrue(countUsersBeforeClear != 0);
            assertEquals(0, countUsersAfterClear);
        }

        @Test
        public void add_duplicate_shouldThrowsIllegalArgumentException() {
            User user = new User("hikaruvi", "Daniil Rybkin", "rybkin.daniil@gmail.com");
            User userSameUsername = new User("hikaruvi", "Danil Kolbasenko", "kolbasenko.rulit@yandex.ru");

            userManager.add(user);

            assertThrows(IllegalArgumentException.class, () -> userManager.add(userSameUsername));
        }

        private static final Random random = new Random();
        private static final String[] allowedUsernamePrefixes = {"hikaruvi", "maickaljacson", "abumba", "mister_ataka"};
        private static final String[] allowedNames = {"Daniil", "Michail", "Vitalik", "Oleg"};
        private static final String[] allowedSurname = {"Rybkin", "Petrow", "Ivanov", "Sidorov"};
        private static final String[] allowedDomains = {"yandex.ru", "gmail.com", "inbox.list"};

        private static User generateUser() {
            UUID uuid = UUID.randomUUID();
            String username = allowedUsernamePrefixes[random.nextInt(allowedUsernamePrefixes.length)] + uuid;
            String name = allowedNames[random.nextInt(allowedNames.length)];
            String surname = allowedSurname[random.nextInt(allowedSurname.length)];
            String domain = allowedDomains[random.nextInt(allowedDomains.length)];

            String email = name.toLowerCase() + "." + surname.toLowerCase() + "@" + domain;
            String fullName = name + " " + surname;

            return new User(username, fullName, email);
        }
    }


    @Nested
    public class TestRole {
        RoleManager roleManager;
        AssignmentManager assignmentManager;
        UserManager userManager;

        @BeforeEach
        public void beforeE() {
            roleManager = new RoleManager();
            assignmentManager = new AssignmentManager();
            userManager = new UserManager();

            roleManager.setAssignmentManager(assignmentManager);
            assignmentManager.setRoleManager(roleManager);
            assignmentManager.setUserManager(userManager);
        }

        @AfterEach
        public void afterE() {
            roleManager.clear();
            assignmentManager.clear();
        }

        @Test
        public void test_remove_default() {
            Role role = new Role("CEO");


            roleManager.add(role);


            assertTrue(roleManager.remove(role));
        }

        @Test
        public void test_remove_shouldReturnFalse() {
            Role role = new Role("CEO");
            User supervisor = new User("hikaruvi", "Daniil Rybkin", "dan.ran@gmail.com");
            User subordinate = new User("dmitriy_malickov", "Dima Blinan", "dd@yandex.ru");
            AssignmentMetadata metadata = AssignmentMetadata.now(supervisor.username(), "Delegate");

            RoleAssignment permanentAssignment = new PermanentAssignment(subordinate, role, metadata);
            assignmentManager.add(permanentAssignment);


            roleManager.add(role);


            assertFalse(roleManager.remove(role));
        }

        @Test
        public void test_clear_shouldDoesNotThrow() {
            Role main = new Role("CEO");
            Role helper = new Role("Manager");
            Role designer = new Role("UX/UI");
            List<Role> roleList = List.of(main, helper, designer);


            for (int i = 0; i < roleList.size(); ++i) {
                roleManager.add(roleList.get(i));
            }


            assertDoesNotThrow(roleManager::clear);
        }

        @Test
        public void test_clear_shouldThrowIllegalStateException() {
            Role main = new Role("CEO");
            Role helper = new Role("Manager");
            Role designer = new Role("UX/UI");
            List<Role> roleList = new ArrayList<>(List.of(main, helper, designer));

            User supervisor = new User("hikaruvi", "Daniil Rybkin", "dan.ran@gmail.com");
            User subordinate = new User("dmitriy_malickov", "Dima Blinan", "dd@yandex.ru");
            AssignmentMetadata metadata = AssignmentMetadata.now(supervisor.username(), "Delegate");

            RoleAssignment permanentAssignment = new PermanentAssignment(subordinate, main, metadata);
            assignmentManager.add(permanentAssignment);


            for (Role role : roleList) {
                roleManager.add(role);
            }


            assertThrows(IllegalStateException.class, roleManager::clear);
        }
    }


    @Nested
    public class TestAssignment {
        private AssignmentManager assignmentManager;
        private UserManager userManager;
        private RoleManager roleManager;

        @BeforeEach
        public void beforeE() {
            assignmentManager = new AssignmentManager();
            userManager = new UserManager();
            roleManager = new RoleManager();

            assignmentManager.setUserManager(userManager);
            assignmentManager.setRoleManager(roleManager);
            roleManager.setAssignmentManager(assignmentManager);
        }

        @AfterEach
        public void afterE() {
            assignmentManager.clear();
            userManager.clear();
            roleManager.clear();
        }

        @Test
        public void test_add_default() {
            Role role = new Role("Frontend Developer");
            roleManager.add(role);

            User supervisor = new User("hikaruvi", "Daniil Rybkin", "dan.ran@gmail.com");
            User subordinate = new User("dmitriy_malickov", "Dima Blinan", "dd@yandex.ru");
            userManager.add(supervisor);
            userManager.add(subordinate);

            AssignmentMetadata metadata = AssignmentMetadata.now(supervisor.username(), "Delegate");

            RoleAssignment permanentAssignment = new PermanentAssignment(subordinate, role, metadata);

            assertDoesNotThrow(() -> assignmentManager.add(permanentAssignment));
        }

        @Test
        public void test_add_ThrowsError() {
            Role role = new Role("Frontend Developer");
            User supervisor = new User("hikaruvi", "Daniil Rybkin", "dan.ran@gmail.com");
            User subordinate = new User("dmitriy_malickov", "Dima Blinan", "dd@yandex.ru");
            AssignmentMetadata metadata = AssignmentMetadata.now(supervisor.username(), "Delegate");

            RoleAssignment permanentAssignment = new PermanentAssignment(subordinate, role, metadata);

            assertThrows(IllegalArgumentException.class, () -> assignmentManager.add(permanentAssignment));
        }
    }
}
