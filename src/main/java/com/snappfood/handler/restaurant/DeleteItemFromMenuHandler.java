package com.snappfood.handler.restaurant;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.google.gson.Gson;
import com.snappfood.Role;
import com.snappfood.model.*;
import com.snappfood.repository.*;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.OutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class DeleteItemFromMenuHandler implements HttpHandler {
    private static final String APPLICATION_JSON = "application/json";
    private static final String SECRET = "YOUR_SECRET_KEY";
    private static final String BEARER_PREFIX = "Bearer ";

    private final Gson gson = new Gson();
    private final JWTVerifier verifier = JWT.require(Algorithm.HMAC256(SECRET)).build();
    private final UserRepository userRepository = new UserRepository();
    private final MenuRepository menuRepository = new MenuRepository();
    private final FoodRepository foodRepository = new FoodRepository();
    private final RestaurantRepository restaurantRepository = new RestaurantRepository();

    private final long restaurantId;
    private final String menuTitle;
    private final long itemId;

    public DeleteItemFromMenuHandler(long restaurantId, String menuTitle, long itemId) {
        this.restaurantId = restaurantId;
        this.menuTitle = menuTitle;
        this.itemId = itemId;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"DELETE".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, Map.of("error", "Method not allowed"));
            return;
        }

        try {
            User seller = authenticate(exchange);
            if (seller == null) return;

            Optional<Restaurant> restaurantOpt = restaurantRepository.findById(restaurantId);
            if (restaurantOpt.isEmpty() || !seller.getId().equals(restaurantOpt.get().getOwner().getId())) {
                sendJson(exchange, 403, Map.of("error", "Unauthorized access to this restaurant"));
                return;
            }

            Optional<Menu> menuOpt = menuRepository.findByTitleAndRestaurantId(menuTitle, restaurantId);
            Optional<Food> foodOpt = foodRepository.findById(itemId);

            if (menuOpt.isEmpty()) {
                sendJson(exchange, 404, Map.of("error", "Menu not found"));
                return;
            }

            if (foodOpt.isEmpty()) {
                sendJson(exchange, 404, Map.of("error", "Food item not found"));
                return;
            }

            Menu menu = menuOpt.get();
            Food food = foodOpt.get();

            List<Food> foodItems = menu.getFoodItems();

            boolean removed = foodItems.removeIf(f -> f.getId().equals(food.getId()));
            if (!removed) {
                sendJson(exchange, 404, Map.of("error", "Food item not part of this menu"));
                return;
            }

            food.setMenu(null);
            foodRepository.update(food);

            menu.setFoodItems(foodItems);
            menuRepository.update(menu);

            sendJson(exchange, 200, Map.of("message", "Item removed from menu"));

        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, 500, Map.of("error", "Internal server error"));
        }
    }

    private User authenticate(HttpExchange exchange) throws IOException {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            sendJson(exchange, 401, Map.of("error", "Missing or invalid Authorization header"));
            return null;
        }

        try {
            String token = authHeader.substring(BEARER_PREFIX.length()).trim();
            DecodedJWT jwt = verifier.verify(token);
            Long userId = Long.parseLong(jwt.getSubject());
            Optional<User> userOpt = userRepository.findById(userId);

            if (userOpt.isEmpty() || userOpt.get().getRole() != Role.SELLER) {
                sendJson(exchange, 403, Map.of("error", "Only sellers can modify menus"));
                return null;
            }

            return userOpt.get();
        } catch (Exception e) {
            sendJson(exchange, 401, Map.of("error", "Invalid or expired token"));
            return null;
        }
    }

    private void sendJson(HttpExchange exchange, int statusCode, Map<String, Object> response) throws IOException {
        byte[] bytes = gson.toJson(response).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", APPLICATION_JSON);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
