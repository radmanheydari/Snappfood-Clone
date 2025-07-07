package com.snappfood.handler.restaurant;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.snappfood.Role;
import com.snappfood.model.*;
import com.snappfood.repository.*;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class AddItemToMenuHandler implements HttpHandler {
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

    public AddItemToMenuHandler(long restaurantId, String menuTitle) {
        this.restaurantId = restaurantId;
        this.menuTitle = menuTitle;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"PUT".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, Map.of("error", "Method not allowed"));
            return;
        }

        try {
            User seller = authenticate(exchange);
            if (seller == null) return;

            Optional<Restaurant> restaurantOpt = restaurantRepository.findById(restaurantId);
            if (restaurantOpt.isEmpty() || !restaurantOpt.get().getOwner().getId().equals(seller.getId())) {
                sendJson(exchange, 403, Map.of("error", "Unauthorized access to this restaurant"));
                return;
            }

            Map<String, Object> body = parseRequestBody(exchange);
            if (body == null || !body.containsKey("item_id")) {
                sendJson(exchange, 400, Map.of("error", "Missing 'item_id' field"));
                return;
            }

            long foodId = ((Number) body.get("item_id")).longValue();
            Optional<Food> foodOpt = foodRepository.findById(foodId);
            if (foodOpt.isEmpty()) {
                sendJson(exchange, 404, Map.of("error", "Food item not found"));
                return;
            }

            Optional<Menu> menuOpt = menuRepository.findByTitleAndRestaurantId(menuTitle, restaurantId);
            if (menuOpt.isEmpty()) {
                sendJson(exchange, 404, Map.of("error", "Menu not found"));
                return;
            }

            Menu menu = menuOpt.get();
            menu.getFoodItems().add(foodOpt.get());//FIXME : THERE IS SOMETHING WRONG WITH THIS! 500 INTERNAL ERROR
            menuRepository.update(menu);

            sendJson(exchange, 200, Map.of("message", "Item added to menu successfully"));
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

    private Map<String, Object> parseRequestBody(HttpExchange exchange) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
            Type type = new TypeToken<Map<String, Object>>() {}.getType();
            return gson.fromJson(reader, type);
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
