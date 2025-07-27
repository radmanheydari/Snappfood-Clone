package com.snappfood.handler.admin;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.google.gson.Gson;
import com.snappfood.model.Order;
import com.snappfood.model.User;
import com.snappfood.repository.OrderRepository;
import com.snappfood.repository.UserRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ViewAllOrdersHandler implements HttpHandler {
    private static final String APPLICATION_JSON = "application/json";
    private static final String SECRET           = "YOUR_SECRET_KEY";
    private static final String BEARER_PREFIX    = "Bearer ";

    private final Gson gson = new Gson();
    private final JWTVerifier verifier = JWT.require(Algorithm.HMAC256(SECRET)).build();
    private final UserRepository userRepo = new UserRepository();
    private final OrderRepository orderRepo = new OrderRepository();

    @Override
    public void handle(HttpExchange exchange) {
        try {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                send(exchange, 405, Map.of("error", "Method not allowed"));
                return;
            }

            String auth = exchange.getRequestHeaders().getFirst("Authorization");
            if (auth == null || !auth.startsWith(BEARER_PREFIX)) {
                send(exchange, 401, Map.of("error", "Missing or invalid Authorization header"));
                return;
            }

            var token = auth.substring(BEARER_PREFIX.length()).trim();
            var jwt = verifier.verify(token);
            long userId = Long.parseLong(jwt.getSubject());
            Optional<User> uopt = userRepo.findById(userId);
            if (uopt.isEmpty() || !"admin".equals(uopt.get().getPhone())) {
                send(exchange, 403, Map.of("error", "Forbidden"));
                return;
            }

            // fetch all orders
            List<Order> orders = orderRepo.findAll();
            send(exchange, 200, orders);

        } catch (com.auth0.jwt.exceptions.JWTVerificationException e) {
            send(exchange, 401, Map.of("error", "Invalid or expired token"));
        } catch (Exception e) {
            e.printStackTrace();
            send(exchange, 500, Map.of("error", "Internal server error"));
        }
    }

    private void send(HttpExchange ex, int status, Object body) {
        try {
            byte[] bytes = gson.toJson(body).getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().set("Content-Type", APPLICATION_JSON + "; charset=UTF-8");
            ex.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(bytes);
            }
        } catch (Exception ignore) {}
    }
}
