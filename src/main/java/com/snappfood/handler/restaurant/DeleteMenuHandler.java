package com.snappfood.handler.restaurant;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.google.gson.Gson;
import com.snappfood.Role;
import com.snappfood.model.Menu;
import com.snappfood.model.Restaurant;
import com.snappfood.model.User;
import com.snappfood.repository.MenuRepository;
import com.snappfood.repository.RestaurantRepository;
import com.snappfood.repository.UserRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Optional;

public class DeleteMenuHandler implements HttpHandler {
    private static final String APPLICATION_JSON = "application/json";
    private static final String SECRET = "YOUR_SECRET_KEY";
    private static final String BEARER_PREFIX = "Bearer ";

    private final Gson gson = new Gson();
    private final JWTVerifier verifier = JWT.require(Algorithm.HMAC256(SECRET)).build();

    private final UserRepository userRepository = new UserRepository();
    private final RestaurantRepository restaurantRepository = new RestaurantRepository();
    private final MenuRepository menuRepository = new MenuRepository();

    private final long restaurantId;
    private final long menuId;

    public DeleteMenuHandler(long restaurantId, long menuId) {
        this.restaurantId = restaurantId;
        this.menuId = menuId;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"DELETE".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "Method not allowed");
            return;
        }

        try {
            User seller = authenticateUser(exchange);
            if (seller == null) return;

            Optional<Restaurant> optionalRestaurant = restaurantRepository.findById(restaurantId);
            if (optionalRestaurant.isEmpty()) {
                sendResponse(exchange, 404, "Restaurant not found");
                return;
            }

            Restaurant restaurant = optionalRestaurant.get();
            if (!restaurant.getOwner().getId().equals(seller.getId())) {
                sendResponse(exchange, 403, "You are not the owner of this restaurant");
                return;
            }

            Optional<Menu> optionalMenu = menuRepository.findById(menuId);
            if (optionalMenu.isEmpty()) {
                sendResponse(exchange, 404, "Menu not found");
                return;
            }

            Menu menu = optionalMenu.get();
            if (!menu.getRestaurant().getId().equals(restaurantId)) {
                sendResponse(exchange, 400, "Menu does not belong to this restaurant");
                return;
            }

            menuRepository.delete(menuId);
            sendResponse(exchange, 200, "Menu deleted successfully");

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
            Long userId = Long.parseLong(jwt.getSubject());
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty() || userOpt.get().getRole() != Role.SELLER) {
                sendResponse(exchange, 403, "Only sellers can delete menus");
                return null;
            }
            return userOpt.get();
        } catch (JWTVerificationException | NumberFormatException e) {
            sendResponse(exchange, 401, "Invalid or expired token");
            return null;
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        String json = gson.toJson(Collections.singletonMap(
                statusCode >= 400 ? "error" : "message", message
        ));
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", APPLICATION_JSON);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
