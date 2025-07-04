package com.snappfood.handler.restaurant;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.snappfood.Role;
import com.snappfood.model.Menu;
import com.snappfood.model.Restaurant;
import com.snappfood.model.User;
import com.snappfood.repository.MenuRepository;
import com.snappfood.repository.RestaurantRepository;
import com.snappfood.repository.UserRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.snappfood.dto.MenuDTO;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

public class AddMenuHandler implements HttpHandler {
    private final long restaurantId;
    private final JWTVerifier verifier = JWT.require(Algorithm.HMAC256("YOUR_SECRET_KEY")).build();
    private final RestaurantRepository restaurantRepository = new RestaurantRepository();
    private final UserRepository userRepository = new UserRepository();
    private final MenuRepository menuRepository = new MenuRepository();
    private final Gson gson = new Gson();

    public AddMenuHandler(long restaurantId) {
        this.restaurantId = restaurantId;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }

        String auth = exchange.getRequestHeaders().getFirst("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            sendJson(exchange, 401, "{\"error\":\"Missing or invalid Authorization header\"}");
            return;
        }

        try {
            String token = auth.substring("Bearer ".length()).trim();
            DecodedJWT jwt = verifier.verify(token);
            Long userId = Long.parseLong(jwt.getSubject());

            Optional<User> optionalUser = userRepository.findById(userId);
            if (optionalUser.isEmpty() || optionalUser.get().getRole() != Role.SELLER) {
                sendJson(exchange, 403, "{\"error\":\"Access denied\"}");
                return;
            }

            Optional<Restaurant> optionalRestaurant = restaurantRepository.findById(restaurantId);
            if (optionalRestaurant.isEmpty()) {
                sendJson(exchange, 404, "{\"error\":\"Restaurant not found\"}");
                return;
            }

            Restaurant restaurant = optionalRestaurant.get();
            if (!restaurant.getOwner().getId().equals(userId)) {
                sendJson(exchange, 403, "{\"error\":\"You do not own this restaurant\"}");
                return;
            }

            Type type = new TypeToken<Map<String, Object>>() {}.getType();
            Map<String, Object> data = new Gson().fromJson(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8), type);

            if (data == null || !data.containsKey("title")) {
                sendJson(exchange, 400, "{\"error\":\"Missing required field: title\"}");
                return;
            }

            Menu menu = new Menu();
            menu.setTitle((String) data.get("title"));
            menu.setRestaurant(restaurant);

            Menu saved = menuRepository.save(menu);
            MenuDTO dto = new MenuDTO(saved);
            sendJson(exchange, 201, gson.toJson(dto));

        } catch (JWTVerificationException e) {
            sendJson(exchange, 401, "{\"error\":\"Invalid or expired token\"}");
        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, 500, "{\"error\":\"Internal server error\"}");
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
