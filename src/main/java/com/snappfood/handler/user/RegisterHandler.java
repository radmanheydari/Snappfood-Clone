package com.snappfood.handler.user;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.google.gson.Gson;
import com.snappfood.Role;
import com.snappfood.model.User;
import com.snappfood.model.BankInfo;
import com.snappfood.repository.UserRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class RegisterHandler implements HttpHandler {
    private final Gson gson = new Gson();
    private final UserRepository userRepository = new UserRepository();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            InputStream is = exchange.getRequestBody();
            String requestBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            Map<String, Object> userData = gson.fromJson(requestBody, Map.class);

            String fullname       = (String) userData.get("full_name");
            String phone          = (String) userData.get("phone");
            String email          = (String) userData.get("email");
            String password       = (String) userData.get("password");
            String roleStr        = (String) userData.get("role");
            String address        = (String) userData.get("address");
            String profilePicture = (String) userData.get("profilePicture");

            if (fullname == null || phone == null || password == null || roleStr == null || address == null) {
                sendResponse(exchange, 400, "{\"error\":\"Missing required field\"}");
                return;
            }

            Role role;
            try {
                role = Role.valueOf(roleStr);
            } catch (IllegalArgumentException ex) {
                sendResponse(exchange, 400, "{\"error\":\"Invalid role. Must be one of BUYER, SELLER, COURIER\"}");
                return;
            }

            if (userRepository.existsByPhone(phone)) {
                sendResponse(exchange, 409, "{\"error\":\"Phone number already exists\"}");
                return;
            }

            String bankName = null;
            String accountNumber = null;
            if (userData.get("bank_info") instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> bankInfoMap = (Map<String, Object>) userData.get("bank_info");
                bankName = (String) bankInfoMap.get("bank_name");
                accountNumber = (String) bankInfoMap.get("account_number");
            }

            User user = new User();
            user.setFull_name(fullname);
            user.setPhone(phone);
            user.setEmail(email);
            user.setPassword(password);
            user.setRole(role);
            user.setAddress(address);
            user.setProfilePicture(profilePicture);

            if (bankName != null || accountNumber != null) {
                BankInfo bankInfo = new BankInfo();
                bankInfo.setBank_name(bankName);
                bankInfo.setAccount_number(accountNumber);
                user.setBank_info(bankInfo);
            }

            User saved = userRepository.save(user);

            String responseJson = String.format(
                    "{\"message\":\"User registered successfully\",\"userId\":%d}",
                    saved.getId()
            );
            sendResponse(exchange, 200, responseJson);

        } catch (Exception ex) {
            ex.printStackTrace();
            String err = String.format("{\"error\":\"Internal server error: %s\"}", ex.getMessage());
            sendResponse(exchange, 500, err);
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }
}
