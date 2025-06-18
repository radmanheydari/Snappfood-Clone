package com.snappfood.handler.restaurant;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.google.gson.Gson;
import com.snappfood.Role;
import com.snappfood.model.Restaurant;
import com.snappfood.model.User;
import com.snappfood.repository.RestaurantRepository;
import com.snappfood.repository.UserRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class GetListOfSellersRestaurantHandler implements HttpHandler {
    private static final String APPLICATION_JSON = "application/json";
    private static final String SECRET = "YOUR_SECRET_KEY";
    private static final String BEARER_PREFIX = "Bearer ";

    private final Gson gson = new Gson();
    private final JWTVerifier verifier = JWT.require(Algorithm.HMAC256(SECRET)).build();
    private final UserRepository userRepository = new UserRepository();
    private final RestaurantRepository restaurantRepository = new RestaurantRepository();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Only GET method is accepted
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJsonResponse(exchange, 405, errorJson("Method not allowed"));
            return;
        }

        // 1. Read Authorization header and validate token
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

        // 2. Extract user ID from token subject
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
            e.printStackTrace();
            sendJsonResponse(exchange, 500, errorJson("Internal server error"));
            return;
        }

        // 3. Only SELLER can access their restaurant list
        if (user.getRole() != Role.SELLER) {
            sendJsonResponse(exchange, 403, errorJson("Only sellers can access their restaurant list"));
            return;
        }

        // 4. Fetch restaurants owned by this seller
        try {
            List<Restaurant> restaurants = restaurantRepository.findByOwner(user);

            // Convert to DTO to avoid circular references and excessive data
            List<RestaurantDTO> restaurantDTOs = restaurants.stream()
                    .map(RestaurantDTO::new)
                    .collect(Collectors.toList());

            sendJsonResponse(exchange, 200, gson.toJson(restaurantDTOs));
        } catch (Exception e) {
            e.printStackTrace();
            sendJsonResponse(exchange, 500, errorJson("Failed to retrieve restaurants"));
        }
    }

    // DTO to avoid exposing unnecessary data and prevent circular references
    private static class RestaurantDTO {
        private final Long id;
        private final String name;
        private final String address;
        private final String phone;
        private final String logoBase64;
        private final int tax_fee;
        private final int additional_fee;

        public RestaurantDTO(Restaurant restaurant) {
            this.id = restaurant.getId();
            this.name = restaurant.getName();
            this.address = restaurant.getAddress();
            this.phone = restaurant.getPhone();
            this.logoBase64 = restaurant.getLogoBase64();
            this.tax_fee = restaurant.getTax_fee();
            this.additional_fee = restaurant.getAdditional_fee();
        }

        // Getters
        public Long getId() { return id; }
        public String getName() { return name; }
        public String getAddress() { return address; }
        public String getPhone() { return phone; }
        public String getLogoBase64() { return logoBase64; }
        public int getTax_fee() { return tax_fee; }
        public int getAdditional_fee() { return additional_fee; }
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