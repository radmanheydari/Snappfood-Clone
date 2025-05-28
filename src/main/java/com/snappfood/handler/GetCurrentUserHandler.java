package com.snappfood.handler;

import com.google.gson.Gson;
import com.snappfood.Role;
import com.snappfood.model.User;
import com.snappfood.repository.UserRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

public class GetCurrentUserHandler implements HttpHandler {
    private final Gson gson = new Gson();
    private final UserRepository userRepository = new UserRepository();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("GET".equals(exchange.getRequestMethod())) {
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

            userRepository.save(user);
            sendResponse(exchange, 200, "User registered successfully");
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