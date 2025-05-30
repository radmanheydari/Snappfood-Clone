package com.snappfood.handler.user;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.google.gson.Gson;
import com.snappfood.dto.LoginDTO.UserData;
import com.snappfood.model.User;
import com.snappfood.repository.UserRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class GetCurrentUserHandler implements HttpHandler {
    private static final String APPLICATION_JSON = "application/json";
    private static final String SECRET = "YOUR_SECRET_KEY";
    private static final String BEARER_PREFIX = "Bearer ";
    private final Gson gson = new Gson();
    private final UserRepository userRepository = new UserRepository();
    private final JWTVerifier verifier = JWT.require(Algorithm.HMAC256(SECRET)).build();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Only allow GET
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJsonResponse(exchange, 405, errorJson("Method not allowed"));
            return;
        }

        // Read Authorization header
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

        // Extract user id
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

        // Map to DTO and return
        UserData dto = new UserData(user);
        String json = gson.toJson(dto);

        exchange.getResponseHeaders().set("Content-Type", APPLICATION_JSON);
        exchange.sendResponseHeaders(200, json.getBytes(StandardCharsets.UTF_8).length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
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
