package com.snappfood.handler.restaurant;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.google.gson.Gson;
import com.snappfood.Role;
import com.snappfood.model.Food;
import com.snappfood.model.Restaurant;
import com.snappfood.model.User;
import com.snappfood.repository.FoodRepository;
import com.snappfood.repository.RestaurantRepository;
import com.snappfood.repository.UserRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class DeleteFoodItemHandler implements HttpHandler {
    private static final String SECRET = "YOUR_SECRET_KEY";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JWTVerifier verifier = JWT.require(Algorithm.HMAC256(SECRET)).build();
    private final Gson gson = new Gson();

    private final UserRepository userRepository = new UserRepository();
    private final RestaurantRepository restaurantRepository = new RestaurantRepository();
    private final FoodRepository foodRepository = new FoodRepository();

    private final long restaurantId;
    private final long foodItemId;

    public DeleteFoodItemHandler(long restaurantId, long foodItemId) {
        this.restaurantId = restaurantId;
        this.foodItemId = foodItemId;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("DELETE")) {
            sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }

        try {
            User seller = authenticate(exchange);
            if (seller == null) return;

            Optional<Restaurant> restaurantOpt = restaurantRepository.findById(restaurantId);
            if (restaurantOpt.isEmpty() || !restaurantOpt.get().getOwner().getId().equals(seller.getId())) {
                sendJson(exchange, 403, "{\"error\":\"You do not own this restaurant\"}");
                return;
            }

            Optional<Food> foodOpt = foodRepository.findById(foodItemId);
            if (foodOpt.isEmpty() || !foodOpt.get().getRestaurant().getId().equals(restaurantId)) {
                sendJson(exchange, 404, "{\"error\":\"Food item not found in this restaurant\"}");
                return;
            }

            foodRepository.delete(foodOpt.get().getId());
            sendJson(exchange, 200, "{\"message\":\"Food item deleted successfully\"}");

        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, 500, "{\"error\":\"Internal server error\"}");
        }
    }

    private User authenticate(HttpExchange exchange) throws IOException {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            sendJson(exchange, 401, "{\"error\":\"Missing or invalid Authorization header\"}");
            return null;
        }

        try {
            String token = authHeader.substring(BEARER_PREFIX.length());
            DecodedJWT jwt = verifier.verify(token);
            long userId = Long.parseLong(jwt.getSubject());

            Optional<User> user = userRepository.findById(userId);
            if (user.isEmpty() || user.get().getRole() != Role.SELLER) {
                sendJson(exchange, 403, "{\"error\":\"Only sellers can delete food items\"}");
                return null;
            }

            return user.get();
        } catch (JWTVerificationException | NumberFormatException e) {
            sendJson(exchange, 401, "{\"error\":\"Invalid or expired token\"}");
            return null;
        }
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
