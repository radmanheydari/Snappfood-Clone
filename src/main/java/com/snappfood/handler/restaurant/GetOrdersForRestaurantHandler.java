package com.snappfood.handler.restaurant;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.google.gson.Gson;
import com.snappfood.Role;
import com.snappfood.dto.OrderDTO;
import com.snappfood.model.Order;
import com.snappfood.model.User;
import com.snappfood.repository.OrderRepository;
import com.snappfood.repository.RestaurantRepository;
import com.snappfood.repository.UserRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.OutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class GetOrdersForRestaurantHandler implements HttpHandler {
    private static final String APPLICATION_JSON = "application/json";
    private static final String SECRET           = "YOUR_SECRET_KEY";
    private static final String BEARER_PREFIX    = "Bearer ";

    private final Gson                gson                 = new Gson();
    private final JWTVerifier         verifier             = JWT.require(Algorithm.HMAC256(SECRET)).build();
    private final UserRepository      userRepo             = new UserRepository();
    private final RestaurantRepository restRepo           = new RestaurantRepository();
    private final OrderRepository     orderRepo            = new OrderRepository();

    private final long restaurantId;

    public GetOrdersForRestaurantHandler(long restaurantId) {
        this.restaurantId = restaurantId;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        String auth = exchange.getRequestHeaders().getFirst("Authorization");
        if (auth == null || !auth.startsWith(BEARER_PREFIX)) {
            sendJson(exchange, 401, Map.of("error","Missing Authorization header"));
            return;
        }
        User seller;
        try {
            DecodedJWT jwt = verifier.verify(auth.substring(BEARER_PREFIX.length()).trim());
            long userId = Long.parseLong(jwt.getSubject());
            Optional<User> uopt = userRepo.findById(userId);
            if (uopt.isEmpty() || uopt.get().getRole() != Role.SELLER) {
                sendJson(exchange, 403, Map.of("error","Forbidden"));
                return;
            }
            seller = uopt.get();
        } catch (Exception e) {
            sendJson(exchange, 401, Map.of("error","Invalid token"));
            return;
        }

        var restOpt = restRepo.findById(restaurantId);
        if (restOpt.isEmpty() || !restOpt.get().getOwner().getId().equals(seller.getId())) {
            sendJson(exchange, 403, Map.of("error","You do not own this restaurant"));
            return;
        }

        try {
            List<Order> orders = orderRepo.findByRestaurantId(restaurantId);
            List<OrderDTO> dtos = orders.stream()
                    .map(OrderDTO::new)
                    .collect(Collectors.toList());
            sendJson(exchange, 200, dtos);
        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, 500, Map.of("error","Internal server error"));
        }
    }

    private void sendJson(HttpExchange exchange, int status, Object payload) throws IOException {
        byte[] bytes = gson.toJson(payload).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", APPLICATION_JSON);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
