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

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public class UpdateRestaurantHandler implements HttpHandler {
    private static final String APPLICATION_JSON = "application/json";
    private static final String SECRET = "YOUR_SECRET_KEY";
    private static final String BEARER_PREFIX = "Bearer ";

    private final Gson gson = new Gson();
    private final JWTVerifier verifier = JWT.require(Algorithm.HMAC256(SECRET)).build();
    private final UserRepository userRepository = new UserRepository();
    private final RestaurantRepository restaurantRepository = new RestaurantRepository();
    private final long restaurantId;

    public UpdateRestaurantHandler(long restaurantId) {
        this.restaurantId = restaurantId;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"PUT".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "Method not allowed");
            return;
        }

        try {
            // 1. Authenticate user
            User seller = authenticateUser(exchange);
            if (seller == null) return;

            // 2. Verify restaurant exists and belongs to seller
            Restaurant restaurant = validateRestaurantOwnership(seller);
            if (restaurant == null) {
                sendResponse(exchange, 404, "Restaurant not found");
                return;
            }

            // 3. Parse and apply updates
            Map<String, Object> updates = parseRequestBody(exchange);
            applyUpdates(restaurant, updates);

            // 4. Save changes
            restaurantRepository.update(restaurant);
            sendResponse(exchange, 200, "Restaurant updated successfully");

        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 500, "Internal server error");
        }
    }

    private User authenticateUser(HttpExchange exchange) throws IOException {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            sendResponse(exchange, 401, "Missing or invalid Authorization header");
            return null;
        }

        try {
            String token = authHeader.substring(BEARER_PREFIX.length()).trim();
            DecodedJWT jwt = verifier.verify(token);
            String userId = jwt.getSubject();

            Optional<User> user = userRepository.findById(Long.parseLong(userId));
            if (user.isEmpty() || user.get().getRole() != Role.SELLER) {
                sendResponse(exchange, 403, "Only sellers can update restaurants");
                return null;
            }
            return user.get();
        } catch (JWTVerificationException e) {
            sendResponse(exchange, 401, "Invalid or expired token");
            return null;
        } catch (NumberFormatException e) {
            sendResponse(exchange, 400, "Invalid user ID in token");
            return null;
        }
    }

    private Restaurant validateRestaurantOwnership(User seller) {
        Optional<Restaurant> restaurant = restaurantRepository.findById(restaurantId);
        if (restaurant.isEmpty() || !restaurant.get().getOwner().getId().equals(seller.getId())) {
            return null;
        }
        return restaurant.get();
    }

    private Map<String, Object> parseRequestBody(HttpExchange exchange) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
            Type type = new TypeToken<Map<String, Object>>() {}.getType();
            return gson.fromJson(reader, type);
        }
    }

    private void applyUpdates(Restaurant restaurant, Map<String, Object> updates) {
        if (updates == null) return;

        if (updates.containsKey("name")) {
            restaurant.setName((String) updates.get("name"));
        }
        if (updates.containsKey("address")) {
            restaurant.setAddress((String) updates.get("address"));
        }
        if (updates.containsKey("phone")) {
            restaurant.setPhone((String) updates.get("phone"));
        }
        if (updates.containsKey("logoBase64")) {
            restaurant.setLogoBase64((String) updates.get("logoBase64"));
        }
        if (updates.containsKey("tax_fee") && updates.get("tax_fee") instanceof Number) {
            restaurant.setTax_fee(((Number) updates.get("tax_fee")).intValue());
        }
        if (updates.containsKey("additional_fee") && updates.get("additional_fee") instanceof Number) {
            restaurant.setAdditional_fee(((Number) updates.get("additional_fee")).intValue());
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        String json = gson.toJson(Collections.singletonMap(
                statusCode >= 400 ? "error" : "message",
                message
        ));
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", APPLICATION_JSON);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}