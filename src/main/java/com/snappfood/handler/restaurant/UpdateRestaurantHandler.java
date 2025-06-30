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
            sendJson(exchange, 405, errorJson("Method not allowed"));
            return;
        }

        try {
            // 1. احراز هویت
            User seller = authenticateSeller(exchange);
            if (seller == null) return;

            // 2. بررسی مالکیت رستوران
            Restaurant restaurant = validateOwnership(seller);
            if (restaurant == null) {
                sendJson(exchange, 403, errorJson("Unauthorized to update this restaurant"));
                return;
            }

            // 3. خواندن داده‌ها
            Map<String, Object> data = readRequestBody(exchange);
            if (data == null) {
                sendJson(exchange, 400, errorJson("Invalid JSON body"));
                return;
            }

            // 4. اعمال تغییرات
            if (data.containsKey("name")) restaurant.setName((String) data.get("name"));
            if (data.containsKey("address")) restaurant.setAddress((String) data.get("address"));
            if (data.containsKey("phone")) restaurant.setPhone((String) data.get("phone"));
            if (data.containsKey("logoBase64")) restaurant.setLogoBase64((String) data.get("logoBase64"));
            if (data.containsKey("tax_fee") && data.get("tax_fee") instanceof Number)
                restaurant.setTax_fee(((Number) data.get("tax_fee")).intValue());
            if (data.containsKey("additional_fee") && data.get("additional_fee") instanceof Number)
                restaurant.setAdditional_fee(((Number) data.get("additional_fee")).intValue());

            // 5. ذخیره در دیتابیس
            restaurantRepository.update(restaurant);

            sendJson(exchange, 200, gson.toJson(Collections.singletonMap("message", "Restaurant updated successfully")));
        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, 500, errorJson("Internal server error"));
        }
    }

    private User authenticateSeller(HttpExchange exchange) throws IOException {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            sendJson(exchange, 401, errorJson("Missing or invalid Authorization header"));
            return null;
        }

        try {
            String token = authHeader.substring(BEARER_PREFIX.length()).trim();
            DecodedJWT jwt = verifier.verify(token);
            long userId = Long.parseLong(jwt.getSubject());

            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty() || userOpt.get().getRole() != Role.SELLER) {
                sendJson(exchange, 403, errorJson("Only sellers can update restaurants"));
                return null;
            }

            return userOpt.get();
        } catch (JWTVerificationException e) {
            sendJson(exchange, 401, errorJson("Invalid or expired token"));
            return null;
        } catch (NumberFormatException e) {
            sendJson(exchange, 400, errorJson("Invalid user ID in token"));
            return null;
        }
    }

    private Restaurant validateOwnership(User seller) {
        Optional<Restaurant> opt = restaurantRepository.findById(restaurantId);
        return (opt.isPresent() && opt.get().getOwner().getId().equals(seller.getId())) ? opt.get() : null;
    }

    private Map<String, Object> readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
            Type type = new TypeToken<Map<String, Object>>() {}.getType();
            return gson.fromJson(reader, type);
        }
    }

    private String errorJson(String message) {
        return gson.toJson(Collections.singletonMap("error", message));
    }

    private void sendJson(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", APPLICATION_JSON);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
