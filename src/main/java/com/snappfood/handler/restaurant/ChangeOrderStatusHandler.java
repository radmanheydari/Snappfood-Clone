package com.snappfood.handler.restaurant;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.snappfood.Role;
import com.snappfood.model.Order;
import com.snappfood.model.User;
import com.snappfood.repository.OrderRepository;
import com.snappfood.repository.UserRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ChangeOrderStatusHandler implements HttpHandler {
    private static final String APPLICATION_JSON = "application/json";
    private static final String SECRET           = "YOUR_SECRET_KEY";
    private static final String BEARER_PREFIX    = "Bearer ";

    private static final Set<String> VALID_STATUSES = Set.of(
            "submitted",
            "unpaid and cancelled",
            "waiting vendor",
            "cancelled",
            "finding courier",
            "on the way",
            "completed"
    );

    private final Gson         gson            = new Gson();
    private final JWTVerifier verifier        = JWT.require(Algorithm.HMAC256(SECRET)).build();
    private final UserRepository  userRepo     = new UserRepository();
    private final OrderRepository orderRepo     = new OrderRepository();
    private final long            orderId;

    public ChangeOrderStatusHandler(long orderId) {
        this.orderId = orderId;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"PUT".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, Map.of("error", "Method not allowed"));
            return;
        }

        String auth = exchange.getRequestHeaders().getFirst("Authorization");
        if (auth == null || !auth.startsWith(BEARER_PREFIX)) {
            sendJson(exchange, 401, Map.of("error", "Missing or invalid Authorization header"));
            return;
        }

        User user;
        try {
            String token = auth.substring(BEARER_PREFIX.length()).trim();
            DecodedJWT jwt = verifier.verify(token);
            long userId = Long.parseLong(jwt.getSubject());
            Optional<User> uopt = userRepo.findById(userId);
            if (uopt.isEmpty() || uopt.get().getRole() != Role.SELLER) {
                sendJson(exchange, 403, Map.of("error", "Only sellers can change order status"));
                return;
            }
            user = uopt.get();
        } catch (Exception e) {
            sendJson(exchange, 401, Map.of("error", "Invalid or expired token"));
            return;
        }

        Optional<Order> oopt = orderRepo.findById(orderId);
        if (oopt.isEmpty()) {
            sendJson(exchange, 404, Map.of("error", "Order not found"));
            return;
        }
        Order order = oopt.get();

        if (order.getVendor().getId() != user.getId()) {
            sendJson(exchange, 403, Map.of("error", "You do not own this order"));
            return;
        }

        Map<String, Object> body;
        try (InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
            Type type = new TypeToken<Map<String, Object>>(){}.getType();
            body = gson.fromJson(reader, type);
        } catch (Exception ex) {
            sendJson(exchange, 400, Map.of("error", "Invalid JSON body"));
            return;
        }

        if (body == null || !body.containsKey("status")) {
            sendJson(exchange, 400, Map.of("error", "Missing 'status' field"));
            return;
        }

        String status = body.get("status").toString().trim();
        if (!VALID_STATUSES.contains(status)) {
            sendJson(exchange, 400, Map.of("error", "Invalid status"));
            return;
        }

        order.setStatus(status);
        orderRepo.update(order);
        sendJson(exchange, 200, Map.of("message", "Order status updated", "status", status));
    }

    private void sendJson(HttpExchange exchange, int code, Map<String, Object> resp) throws IOException {
        byte[] bytes = gson.toJson(resp).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", APPLICATION_JSON);
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
