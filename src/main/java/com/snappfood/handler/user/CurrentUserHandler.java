package com.snappfood.handler.user;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.snappfood.dto.LoginDTO.UserData;
import com.snappfood.model.BankInfo;
import com.snappfood.model.User;
import com.snappfood.repository.UserRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class CurrentUserHandler implements HttpHandler {
    private static final String APPLICATION_JSON = "application/json";
    private static final String SECRET = "YOUR_SECRET_KEY";
    private static final String BEARER_PREFIX = "Bearer ";
    private final Gson gson = new Gson();
    private final UserRepository userRepository = new UserRepository();
    private final JWTVerifier verifier = JWT.require(Algorithm.HMAC256(SECRET)).build();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod().toUpperCase();
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod()) && !"PUT".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJsonResponse(exchange, 405, errorJson("Method not allowed"));
            return;
        }

        String auth = exchange.getRequestHeaders().getFirst("Authorization");
        if (auth == null || !auth.startsWith(BEARER_PREFIX)) {
            sendJsonResponse(exchange, 401, errorJson("Missing or invalid Authorization header"));
            return;
        }

        String token = auth.substring(BEARER_PREFIX.length()).trim();
        DecodedJWT jwt;
        try {
            jwt = verifier.verify(token);
        } catch (JWTVerificationException e) {
            sendJsonResponse(exchange, 401, errorJson("Invalid or expired token"));
            return;
        }

        String sub = jwt.getSubject();
        if (sub == null) {
            sendJsonResponse(exchange, 401, errorJson("Invalid token payload"));
            return;
        }

        User user;
        try {
            long userId = Long.parseLong(sub);
            Optional<User> optionalUser = userRepository.findById(userId);
            if (optionalUser.isEmpty()) {
                sendJsonResponse(exchange, 404, errorJson("User not found"));
                return;
            }
            user = optionalUser.get();
        } catch (NumberFormatException e) {
            sendJsonResponse(exchange, 400, errorJson("Invalid user ID in token"));
            return;
        } catch (Exception e) {
            sendJsonResponse(exchange, 500, errorJson("Internal server error"));
            return;
        }

        if ("GET".equals(method)) {
            handleGet(exchange, user);
        } else if ("PUT".equals(method)) {
            handlePut(exchange, user);
        }
    }

    private void handleGet(HttpExchange exchange, User user) throws IOException {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", user.getId());
        userMap.put("full_name", user.getFull_name());
        userMap.put("phone", user.getPhone());
        userMap.put("email", user.getEmail());
        userMap.put("role", user.getRole().name());
        userMap.put("address", user.getAddress());
        userMap.put("profilePicture", user.getProfilePicture());
        BankInfo bankInfo = user.getBank_info();
        if (bankInfo != null) {
            Map<String, Object> bankMap = new HashMap<>();
            bankMap.put("bank_name", bankInfo.getBank_name());
            bankMap.put("account_number", bankInfo.getAccount_number());
            userMap.put("bank_info", bankMap);
        } else {
            userMap.put("bank_info", null);
        }
        String json = gson.toJson(userMap);
        sendJsonResponse(exchange, 200, json);
    }

    private void handlePut(HttpExchange exchange, User user) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
            Type type = new TypeToken<Map<String, Object>>() {}.getType();
            Map<String, Object> data = gson.fromJson(reader, type);

            if (data != null) {
                if (data.containsKey("full_name")) {
                    user.setFull_name((String) data.get("full_name"));
                }
                if (data.containsKey("phone")) {
                    user.setPhone((String) data.get("phone"));
                }
                if (data.containsKey("email")) {
                    user.setEmail((String) data.get("email"));
                }
                if (data.containsKey("address")) {
                    user.setAddress((String) data.get("address"));
                }
                if (data.containsKey("profilePicture")) {
                    user.setProfilePicture((String) data.get("profilePicture"));
                }
                if (data.containsKey("bank_name")) {
                    user.getBank_info().setBank_name((String) data.get("bank_name"));
                }
                if (data.containsKey("account_number")) {
                    user.getBank_info().setAccount_number((String) data.get("account_number"));
                }
            }

            userRepository.save(user);
            sendJsonResponse(exchange, 200, "{\"message\":\"Profile updated successfully\"}");
        } catch (Exception e) {
            sendJsonResponse(exchange, 400, errorJson("Invalid request body"));
        }
    }

    private String errorJson(String message) {
        Map<String, String> err = new HashMap<>();
        err.put("error", message);
        return gson.toJson(err);
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", APPLICATION_JSON);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
