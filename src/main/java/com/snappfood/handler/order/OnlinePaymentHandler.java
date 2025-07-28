package com.snappfood.handler.order;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.snappfood.Role;
import com.snappfood.model.Order;
import com.snappfood.repository.OrderRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

public class OnlinePaymentHandler implements HttpHandler {
    private static final String APPLICATION_JSON = "application/json";
    private static final String SECRET           = "YOUR_SECRET_KEY";
    private static final String BEARER_PREFIX    = "Bearer ";

    private final Gson           gson      = new Gson();
    private final JWTVerifier    verifier  = JWT.require(Algorithm.HMAC256(SECRET)).build();
    private final OrderRepository orderRepo = new OrderRepository();

    @Override
    public void handle(HttpExchange exchange) {
        try {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                send(exchange, 405, Map.of("error", "Method not allowed"));
                return;
            }

            String auth = exchange.getRequestHeaders().getFirst("Authorization");
            if (auth == null || !auth.startsWith(BEARER_PREFIX)) {
                send(exchange, 401, Map.of("error", "Missing or invalid Authorization header"));
                return;
            }

            long userId;
            try {
                var jwt = verifier.verify(auth.substring(BEARER_PREFIX.length()).trim());
                userId = Long.parseLong(jwt.getSubject());
                if (!Role.BUYER.name().equals(jwt.getClaim("role").asString())) {
                    send(exchange, 403, Map.of("error", "Only buyers can pay"));
                    return;
                }
            } catch (JWTVerificationException e) {
                send(exchange, 401, Map.of("error", "Invalid or expired token"));
                return;
            }

            Type mapType = new TypeToken<Map<String, Long>>(){}.getType();
            Map<String, Long> body = gson.fromJson(
                    new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8),
                    mapType
            );
            Long orderId = body.get("order_id");
            if (orderId == null) {
                send(exchange, 400, Map.of("error", "Missing field: order_id"));
                return;
            }

            Optional<Order> optOrder = orderRepo.findById(orderId);
            if (optOrder.isEmpty()) {
                send(exchange, 404, Map.of("error", "Order not found"));
                return;
            }
            Order order = optOrder.get();
            if (!order.getCustomer().getId().equals(userId)) {
                send(exchange, 403, Map.of("error", "Cannot pay for this order"));
                return;
            }
            if (!"submitted".equals(order.getStatus())) {
                send(exchange, 409, Map.of("error", "Order not in payable state"));
                return;
            }

            order.setStatus("paid");
            orderRepo.update(order);

            send(exchange, 200, Map.of(
                    "message", "Payment successful",
                    "order_id", order.getId(),
                    "status", order.getStatus()
            ));

        } catch (Exception e) {
            e.printStackTrace();
            send(exchange, 500, Map.of("error", "Internal server error"));
        }
    }

    private void send(HttpExchange ex, int status, Object payload) {
        try {
            String json = gson.toJson(payload);
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().set("Content-Type", APPLICATION_JSON + "; charset=UTF-8");
            ex.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(bytes);
            }
        } catch (Exception ignore) {}
    }
}
