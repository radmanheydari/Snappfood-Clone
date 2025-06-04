package com.snappfood.handler.restaurant;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.snappfood.Role;
import com.snappfood.model.Restaurant;
import com.snappfood.model.User;
import com.snappfood.repository.RestaurantRepository;
import com.snappfood.repository.UserRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public class CreateNewRestaurantHandler implements HttpHandler {
    private static final String APPLICATION_JSON = "application/json";
    private static final String SECRET = "YOUR_SECRET_KEY";
    private static final String BEARER_PREFIX = "Bearer ";
    private final Gson gson = new Gson();
    private final JWTVerifier verifier = JWT.require(Algorithm.HMAC256(SECRET)).build();
    private final UserRepository userRepository = new UserRepository();
    private final RestaurantRepository restaurantRepository = new RestaurantRepository();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJsonResponse(exchange, 405, errorJson("Method not allowed"));
            return;
        }

        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            sendJsonResponse(exchange, 401, errorJson("Missing or invalid Authorization header"));
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).trim();
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
        } catch (Exception e) {
            sendJsonResponse(exchange, 400, errorJson("Invalid user ID in token"));
            return;
        }

        if (user.getRole() != Role.SELLER) {
            sendJsonResponse(exchange, 403, errorJson("Only sellers can create restaurants"));
            return;
        }

        try (InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
            Type type = new TypeToken<Map<String, Object>>() {}.getType();
            Map<String, Object> data = gson.fromJson(reader, type);

            if (data == null
                    || !data.containsKey("name")
                    || !data.containsKey("address")
                    || !data.containsKey("phone")) {
                sendJsonResponse(exchange, 400, errorJson("Missing required fields"));
                return;
            }

            Restaurant restaurant = new Restaurant();
            restaurant.setName((String) data.get("name"));
            restaurant.setAddress((String) data.get("address"));
            restaurant.setPhone((String) data.get("phone"));
            restaurant.setOwner(user);

            if (data.containsKey("logoBase64")) {
                restaurant.setLogoBase64((String) data.get("logoBase64"));
            }
            if (data.containsKey("tax_fee")) {
                restaurant.setTax_fee(((Number) data.get("tax_fee")).intValue());
            }
            if (data.containsKey("additional_fee")) {
                restaurant.setAdditional_fee(((Number) data.get("additional_fee")).intValue());
            }

            restaurantRepository.save(restaurant);
            sendJsonResponse(exchange, 201, gson.toJson(Collections.singletonMap("message", "Restaurant created successfully")));
        } catch (Exception e) {
            e.printStackTrace();
            sendJsonResponse(exchange, 500, errorJson("Internal server error"));
        }
    }

    private String errorJson(String message) {
        return gson.toJson(Collections.singletonMap("error", message));
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
