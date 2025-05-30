package com.snappfood.handler.user;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.util.Map;
import com.google.gson.Gson;
import com.snappfood.Role;
import com.snappfood.model.User;
import com.snappfood.repository.UserRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class RegisterHandler implements HttpHandler {
    private final Gson gson = new Gson();
    private final UserRepository userRepository = new UserRepository();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("POST".equals(exchange.getRequestMethod())) {
            InputStream is = exchange.getRequestBody();
            String requestBody = new String(is.readAllBytes());
            Map<String, String> userData = gson.fromJson(requestBody, Map.class);

            String fullname = userData.get("fullname");
            String phone = userData.get("phone");
            String email = userData.get("email");
            String password = userData.get("password");
            String role = userData.get("role");
            String address = userData.get("address");
            String profilePricture = userData.get("profilePricture");
            String bankName = userData.get("bankName");
            String accountNumber = userData.get("accountNumber");

            if (fullname == null || phone == null || password == null || role == null || address == null) {
                sendResponse(exchange, 400, "invalid input");
                return;
            }

            if(!role.equals("SELLER") && !role.equals("BUYER") && !role.equals("COURIER")) {
                sendResponse(exchange, 400, "invalid input");
            }

            if(userRepository.existsByPhone(phone)) {
                sendResponse(exchange, 409, "Phone number already exists");
                return;
            }

            User user = new User();
            user.setFullname(fullname);
            user.setPhone(phone);
            user.setEmail(email);
            user.setPassword(password);
            user.setRole(Role.valueOf(role));
            user.setAddress(address);
            user.setProfilePicture(profilePricture);
            user.setBankName(bankName);
            user.setAccountNumber(accountNumber);

            User tmp = userRepository.save(user);
            String response = String.format("{\\\"message\\\": \\\"User registered successfully\\\", \\\"userId\\\": %d}", tmp.getId());
            sendResponse(exchange, 200, response);
        } else {
            sendResponse(exchange, 405, "Method not allowed");
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        exchange.sendResponseHeaders(statusCode, message.length());
        OutputStream os = exchange.getResponseBody();
        os.write(message.getBytes());
        os.close();
    }
}