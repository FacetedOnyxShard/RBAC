package org.example;

import java.util.*;

public class UserManager implements Repository<User> {
    private final Map<String, User> users; // ключ username

    public UserManager() {
        users = new HashMap<>();
    }

    @Override
    public void add(User item) {
        if (item == null) throw new IllegalArgumentException();
        if (users.containsKey(item.username())) {
            throw new IllegalArgumentException("Duplicate");
        }
        users.put(item.username(), item);
    }

    @Override
    public boolean remove(User item) {
        if (!users.containsKey(item.username())) {
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

    List<User> findByFilter(UserFilter filter) {
        return users.values().stream()
                .filter(filter::test)
                .toList();
    }

    List<User> findAll(UserFilter filter, Comparator<User> sorter) {
        return users.values().stream()
                .filter(filter::test)
                .sorted(sorter).toList();
    }

    boolean exists(String username) {
        return users.containsKey(username.trim());
    }

    void update(String username, String newFullName, String newEmail) {
        if (username == null || newFullName == null || newEmail == null) {
            throw new IllegalArgumentException();
        }

        String usernameNormalized = username.trim();

        if (!users.containsKey(usernameNormalized)) {
            throw new IllegalArgumentException("User not exists");
        }

        User userUpdated = new User(usernameNormalized, newFullName, newEmail);
        users.replace(usernameNormalized, userUpdated);
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
