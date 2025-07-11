// com/snappfood/handler/restaurant/GetOrdersForRestaurantHandler.java
package com.snappfood.handler.restaurant;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.google.gson.Gson;
import com.snappfood.Role;
import com.snappfood.dto.OrderDTO;
import com.snappfood.model.Order;
import com.snappfood.model.Restaurant;
import com.snappfood.model.User;
import com.snappfood.repository.OrderRepository;
import com.snappfood.repository.RestaurantRepository;
import com.snappfood.repository.UserRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.OutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class GetOrdersForRestaurantHandler implements HttpHandler {
    private static final String APPLICATION_JSON = "application/json";
    private static final String SECRET           = "YOUR_SECRET_KEY";
    private static final String BEARER_PREFIX    = "Bearer ";

    private final Gson                gson                 = new Gson();
    private final JWTVerifier         verifier             = JWT.require(Algorithm.HMAC256(SECRET)).build();
    private final UserRepository      userRepository       = new UserRepository();
    private final RestaurantRepository restaurantRepository = new RestaurantRepository();
    private final OrderRepository     orderRepository      = new OrderRepository();

    private final long restaurantId;

    public GetOrdersForRestaurantHandler(long restaurantId) {
        this.restaurantId = restaurantId;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, Map.of("error", "Method not allowed"));
            return;
        }

        // Authenticate seller
        String auth = exchange.getRequestHeaders().getFirst("Authorization");
        if (auth == null || !auth.startsWith(BEARER_PREFIX)) {
            sendJson(exchange, 401, Map.of("error", "Missing or invalid Authorization header"));
            return;
        }
        User seller;
        try {
            DecodedJWT jwt = verifier.verify(auth.substring(BEARER_PREFIX.length()).trim());
            long userId = Long.parseLong(jwt.getSubject());
            Optional<User> u = userRepository.findById(userId);
            if (u.isEmpty() || u.get().getRole() != Role.SELLER) {
                sendJson(exchange, 403, Map.of("error", "Access denied"));
                return;
            }
            seller = u.get();
        } catch (Exception e) {
            sendJson(exchange, 401, Map.of("error", "Invalid or expired token"));
            return;
        }

        // Verify restaurant ownership
        Optional<Restaurant> restOpt = restaurantRepository.findById(restaurantId);
        if (restOpt.isEmpty() || !restOpt.get().getOwner().getId().equals(seller.getId())) {
            sendJson(exchange, 403, Map.of("error", "You do not own this restaurant"));
            return;
        }

        // Fetch orders and map to DTOs
        try {
            List<Order> orders = orderRepository.findByRestaurantId(restaurantId);
            List<OrderDTO> dtos = orders.stream()
                    .map(OrderDTO::new)
                    .collect(Collectors.toList());
            sendJson(exchange, 200, dtos);
        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, 500, Map.of("error", "Internal server error"));
        }
    }

    private void sendJson(HttpExchange exchange, int statusCode, Object payload) throws IOException {
        byte[] bytes = gson.toJson(payload).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", APPLICATION_JSON);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
