package com.snappfood.service;

import com.snappfood.model.User;
import com.snappfood.repository.UserRepository;
import java.util.List;
import java.util.Optional;

public class UserService {

    private final UserRepository userRepository = new UserRepository();

    public User createUser(User user) {
        return userRepository.save(user);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public User updateUser(User user) {
        return userRepository.update(user);
    }

    public void deleteUser(Long id) {
        userRepository.delete(id);
    }
}