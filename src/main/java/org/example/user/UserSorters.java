package org.example.user;

import java.util.Comparator;

public class UserSorters {
    Comparator<User> byUsername() {
        return Comparator.comparing(User::username,
                Comparator.nullsLast(String::compareTo));
    }

    Comparator<User> byFullName() {
        return Comparator.comparing(User::fullName,
                Comparator.nullsLast(String::compareTo));
    }

    Comparator<User> byEmail() {
        return Comparator.comparing(User::email,
                Comparator.nullsLast(String::compareTo));
    }
}
