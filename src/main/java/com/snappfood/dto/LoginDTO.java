package com.snappfood.dto;

import com.snappfood.Role;
import com.snappfood.model.User;

public class LoginDTO {
    private String message;
    private String token;
    private UserData user;

    public LoginDTO(String message, String token, UserData user) {
        this.message = message;
        this.token = token;
        this.user = user;
    }

    // کلاس داخلی برای داده‌های کاربر
    public static class UserData {
        private final Long id;
        private final String fullname;
        private final String phone;
        private final String email;
        private final Role role;
        private final String address;
        private final String profilePicture;
        private final String bankName;
        private final String accountNumber;

        public UserData(User user) {
            this.id = user.getId();
            this.fullname = user.getFullname();
            this.phone = user.getPhone();
            this.email = user.getEmail();
            this.role = user.getRole();
            this.address = user.getAddress();
            this.profilePicture = user.getProfilePicture();
            this.bankName = user.getBankName();
            this.accountNumber = user.getAccountNumber();
        }

        // Getters
        public Long getId() { return id; }
        public String getFullname() { return fullname; }
        public String getPhone() { return phone; }
        public String getEmail() { return email; }
        public Role getRole() { return role; }
        public String getAddress() { return address; }
        public String getProfilePicture() { return profilePicture; }
        public String getBankName() { return bankName; }
        public String getAccountNumber() { return accountNumber; }
    }

    // Getters
    public String getMessage() { return message; }
    public UserData getUser() { return user; }
}