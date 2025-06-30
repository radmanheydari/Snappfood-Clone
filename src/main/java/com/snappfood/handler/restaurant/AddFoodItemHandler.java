package com.snappfood.handler.restaurant;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.snappfood.Role;
import com.snappfood.model.Food;
import com.snappfood.model.Restaurant;
import com.snappfood.model.User;
import com.snappfood.repository.FoodRepository;
import com.snappfood.repository.RestaurantRepository;
import com.snappfood.repository.UserRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class AddFoodItemHandler implements HttpHandler {
    private static final String SECRET = "YOUR_SECRET_KEY";
    private static final String BEARER_PREFIX = "Bearer ";
    private final Gson gson = new Gson();
    private final JWTVerifier verifier = JWT.require(Algorithm.HMAC256(SECRET)).build();
    private final UserRepository userRepository = new UserRepository();
    private final RestaurantRepository restaurantRepository = new RestaurantRepository();
    private final FoodRepository foodRepository = new FoodRepository();
    private final long restaurantId;

    public AddFoodItemHandler(long restaurantId) {
        this.restaurantId = restaurantId;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            sendJson(exchange, 405, errorJson("Method not allowed"));
            return;
        }

        try {
            User seller = authenticateSeller(exchange);
            if (seller == null) return;

            Restaurant restaurant = validateOwnership(seller);
            if (restaurant == null) {
                sendJson(exchange, 403, errorJson("Not authorized to add food to this restaurant"));
                return;
            }

            Map<String, Object> data = readJson(exchange);
            if (data == null) {
                sendJson(exchange, 400, errorJson("Invalid JSON"));
                return;
            }

            Food food = new Food();
            food.setName((String) data.get("name"));
            food.setDescription((String) data.get("description"));
            food.setPrice(((Number) data.get("price")).intValue());
            food.setImageBase64((String) data.get("imageBase64"));
            food.setRestaurant(restaurant);

            if (data.get("keywords") instanceof List<?>) {
                List<String> keywords = (List<String>) data.get("keywords");
                food.setKeywords(new ArrayList<>(keywords));
            }

            foodRepository.save(food);
            sendJson(exchange, 201, gson.toJson(Map.of("message", "Food item added successfully")));

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
                sendJson(exchange, 403, errorJson("Only sellers can add food"));
                return null;
            }

            return userOpt.get();
        } catch (JWTVerificationException | NumberFormatException e) {
            sendJson(exchange, 401, errorJson("Invalid token"));
            return null;
        }
    }

    private Restaurant validateOwnership(User seller) {
        Optional<Restaurant> opt = restaurantRepository.findById(restaurantId);
        return (opt.isPresent() && opt.get().getOwner().getId().equals(seller.getId())) ? opt.get() : null;
    }

    private Map<String, Object> readJson(HttpExchange exchange) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
            Type type = new TypeToken<Map<String, Object>>() {}.getType();
            return gson.fromJson(reader, type);
        }
    }

    private String errorJson(String message) {
        return gson.toJson(Map.of("error", message));
    }

    private void sendJson(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
