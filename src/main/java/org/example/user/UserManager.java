package org.example.user;

import org.example.core.Repository;
import org.example.util.ConsoleUtils;
import org.example.util.ValidationUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class UserManager implements Repository<User> {
    private final Map<String, User> users; // ключ username

    public UserManager() {
        users = new ConcurrentHashMap<>();
    }

    @Override
    public void add(User item) {
        if (item == null)
            throw new IllegalArgumentException("expected User, found null");

        if (!ValidationUtils.isValidUsername(item.username())) {
            throw new IllegalArgumentException("Incorrect username");
        }
        if (!ValidationUtils.isValidEmail(item.email())) {
            throw new IllegalArgumentException("Incorrect email");
        }

        User previous = users.putIfAbsent(item.username(), item);
        if (previous != null) {
            throw new IllegalArgumentException("Duplicate");
        }
    }

    @Override
    public boolean remove(User item) {
        if (item == null) {
            return false;
        }
        return users.remove(item.username(), item);
    }

    @Override
    public Optional<User> findById(String id) {
        return Optional.ofNullable(users.get(id));
    }

    public Optional<User> findByUsername(String username) {
        return Optional.ofNullable(users.get(username));
    }

    public Optional<User> findByEmail(String email) {
        UserFilter userFilter = UserFilters.byEmail(email);
        return users.values().stream()
                .filter(userFilter::test)
                .findFirst();
    }

    public List<User> findByFilter(UserFilter filter) {
        return users.values().stream()
                .filter(filter::test)
                .toList();
    }

    public List<User> findAll(UserFilter filter, Comparator<User> sorter) {
        return users.values().stream()
                .filter(filter::test)
                .sorted(sorter).toList();
    }

    public boolean exists(String username) {
        return users.containsKey(ValidationUtils.normalizeString(username));
    }

    public void update(String username, String newFullName, String newEmail) {
        if (username == null || newFullName == null || newEmail == null) {
            throw new IllegalArgumentException();
        }

        final String usernameNormalized = ValidationUtils.normalizeString(username);

        users.compute(usernameNormalized, (key, oldUser) -> {
            if (oldUser == null) {
                throw new IllegalArgumentException("User not exists: " + usernameNormalized);
            }
            return User.validate(usernameNormalized, newFullName, newEmail);
        });
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(users.values());
    }

    @Override
    public int count() {
        return users.size();
    }

    @Override
    public void clear() {
        users.clear();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass())  return false;

        UserManager entity = (UserManager) obj;
        return Objects.equals(users, entity.users);
    }

    @Override
    public int hashCode() {
        return Objects.hash(users);
    }
}
