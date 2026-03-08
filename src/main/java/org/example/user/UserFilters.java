package org.example.user;


public abstract class UserFilters implements UserFilter {
    public static UserFilter byUsername(String username) {
        return user -> user.username().equals(username);
    }

    public static UserFilter byUsernameContains(String substring) {
        return user-> user.username().contains(substring);
    }

    public static UserFilter byEmail(String email) {
        return user->user.email().equals(email);
    }

    public static UserFilter byEmailContains(String email) {
        return user->user.email().contains(email);
    }

    public static UserFilter byEmailDomain(String domain) {
        return user -> {
            String[] parts = user.email().split("@");
            String domainPart = parts[1];
            return domainPart.equals(domain);
        };
    }

    public static UserFilter byFullNameContains(String substring) {
        return user -> user.fullName().contains(substring);
    }
}
