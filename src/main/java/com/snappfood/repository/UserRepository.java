package com.snappfood.repository;

import com.snappfood.model.User;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class UserRepository {
    private final Map<Long, User> users = new HashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);
    private final Map<String, Long> emails = new HashMap<>();
    private final Map<String, Long> phones = new HashMap<>();

    public User save(User user) {
        if (user.getId() == null) {
            user.setId(idCounter.getAndIncrement());
        }
        users.put(user.getId(), user);
        emails.put(user.getEmail(), user.getId());
        phones.put(user.getPhone(), user.getId());
        return user;
    }

    public Optional<User> findById(Long id) {
        return Optional.ofNullable(users.get(id));
    }

    public List<User> findAll() {
        return new ArrayList<>(users.values());
    }

    public User update(User user) {
        if (users.containsKey(user.getId())) {
            User existingUser = users.get(user.getId());
            emails.remove(existingUser.getEmail());
            emails.put(user.getEmail(), user.getId());
            phones.remove(user.getPhone());
            phones.put(user.getPhone(), user.getId());
            users.put(user.getId(), user);
            return user;
        }
        throw new IllegalArgumentException("User not found with id: " + user.getId());
    }

    public void delete(Long id) {
        User user = users.get(id);
        if (user != null) {
            emails.remove(user.getEmail());
            users.remove(id);
        }
    }

    public boolean existsByEmail(String email) {
        return emails.containsKey(email);
    }
    public boolean existsByPhone(String email) {
        return users.containsKey(email);
    }



    public Optional<User> findByEmail(String email) {
        return Optional.ofNullable(emails.get(email))
                .map(users::get);
    }
}