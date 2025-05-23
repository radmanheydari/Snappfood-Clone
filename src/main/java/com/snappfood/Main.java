package com.snappfood;

import com.snappfood.model.User;
import com.snappfood.repository.UserRepository;

public class Main {
    public static void main(String[] args) {
        User user = new User();
        user.setFullname("John Doe");
        user.setPassword("1234");
        user.setPhone("09123456789");
        user.setRole(Role.BUYER);
        user.setAddress("Tehran");
        user.setProfilePicture("test.png");
        user.setBankName("mellat");
        user.setAccountNumber("123");

        UserRepository userRepository = new UserRepository();
        userRepository.save(user);
        System.out.println("User saved successfully!");
    }
}