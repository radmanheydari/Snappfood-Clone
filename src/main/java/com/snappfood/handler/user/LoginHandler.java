package com.snappfood.handler.user;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.google.gson.Gson;
import com.snappfood.dto.LoginDTO;
import com.snappfood.model.User;
import com.snappfood.repository.UserRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

public class LoginHandler implements HttpHandler {
    private final Gson gson = new Gson();
    private final UserRepository userRepository = new UserRepository();
    private static final String APPLICATION_JSON = "application/json";
    private static final String SECRET = "YOUR_SECRET_KEY";
    private final   Algorithm algorithm = Algorithm.HMAC256(SECRET);

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("POST".equals(exchange.getRequestMethod())) {
            InputStream is = exchange.getRequestBody();
            String requestBody = new String(is.readAllBytes());
            Map<String, String> userData = gson.fromJson(requestBody, Map.class);

            String phone = userData.get("phone");
            String password = userData.get("password");

            if (phone == null || password == null) {
                sendResponse(exchange, 400, "{\\\"error\\\": \\\"invalid input\\\"}");
                return;
            }
            User user = userRepository.findByPhone(phone).get();
            if(!user.getPassword().equals(password)){
                sendResponse(exchange, 401, "{\\\"error\\\": \\\"unauthorized\\\"}");
                return;
            }

            String token;
            try {
                Date now = new Date();
                Date expiresAt = new Date(now.getTime() + 3600_000); // 1 hour expiration
                token = JWT.create()
                        .withSubject(String.valueOf(user.getId()))
                        .withClaim("role", user.getRole().toString())
                        .withIssuedAt(now)
                        .withExpiresAt(expiresAt)
                        .sign(algorithm);
            } catch (Exception e) {
                sendResponse(exchange, 500, "{\\\"error\\\": \\\"Internal Server Error\\\"}");
                return;
            }

            exchange.getResponseHeaders().set("Authorization", "Bearer " + token);
            LoginDTO response = new LoginDTO(
                    "user logged in successfully",
                    token,
                    new LoginDTO.UserData(user)
            );
            sendSuccessResponse(exchange, 200, response);

        } else {
            sendResponse(exchange, 405, "{\\\"error\\\": \\\"Method not allowed\\\"}");
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        exchange.sendResponseHeaders(statusCode, message.length());
        OutputStream os = exchange.getResponseBody();
        os.write(message.getBytes());
        os.close();
    }

    private void sendSuccessResponse(HttpExchange exchange, int statusCode, Object response)
            throws IOException {

        String json = gson.toJson(response);
        sendJsonResponse(exchange, statusCode, json);
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, String json)
            throws IOException {

        byte[] responseBytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
}