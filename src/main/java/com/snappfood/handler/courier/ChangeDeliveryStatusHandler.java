package com.snappfood.handler.courier;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
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

public class ChangeDeliveryStatusHandler implements HttpHandler {
    private static final String JSON = "application/json";
    private static final String SECRET = "YOUR_SECRET_KEY";
    private static final String BEARER = "Bearer ";

    private final Gson gson = new Gson();
    private final JWTVerifier verifier = JWT.require(Algorithm.HMAC256(SECRET)).build();
    private final OrderRepository orders = new OrderRepository();

    @Override
    public void handle(HttpExchange ex) {
        try {
            if (!"PATCH".equalsIgnoreCase(ex.getRequestMethod())) {
                send(ex, 405, Map.of("error","Method not allowed"));
                return;
            }

            String auth = ex.getRequestHeaders().getFirst("Authorization");
            if (auth == null || !auth.startsWith(BEARER)) {
                send(ex, 401, Map.of("error","Missing or invalid Authorization header"));
                return;
            }

            // verify token + role
            var jwt = verifier.verify(auth.substring(BEARER.length()).trim());
            if (!Role.COURIER.name().equals(jwt.getClaim("role").asString())) {
                send(ex, 403, Map.of("error","Only couriers can change delivery status"));
                return;
            }

            // parse ID
            String[] p = ex.getRequestURI().getPath().split("/");
            long orderId = Long.parseLong(p[2]);

            // parse body
            Type mapType = new TypeToken<Map<String,String>>(){}.getType();
            Map<String,String> body;
            try (InputStreamReader r = new InputStreamReader(ex.getRequestBody(), StandardCharsets.UTF_8)) {
                body = gson.fromJson(r, mapType);
            }
            String newStatus = body.get("status");
            if (newStatus == null || newStatus.isBlank()) {
                send(ex, 400, Map.of("error","Field 'status' is required"));
                return;
            }

            // load & update
            Optional<Order> opt = orders.findById(orderId);
            if (opt.isEmpty()) {
                send(ex, 404, Map.of("error","Order not found"));
                return;
            }
            Order o = opt.get();
            o.setStatus(newStatus);
            orders.update(o);

            // simplified success response
            send(ex, 200, Map.of(
                    "orderId", orderId,
                    "status", newStatus
            ));

        } catch (com.auth0.jwt.exceptions.JWTVerificationException e) {
            send(ex, 401, Map.of("error","Invalid or expired token"));
        } catch (NumberFormatException e) {
            send(ex, 400, Map.of("error","Invalid order ID"));
        } catch (Exception e) {
            e.printStackTrace();
            send(ex, 500, Map.of("error","Internal server error"));
        }
    }

    private void send(HttpExchange ex, int status, Object body) {
        try {
            byte[] b = gson.toJson(body).getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().set("Content-Type", JSON+"; charset=UTF-8");
            ex.sendResponseHeaders(status, b.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(b);
            }
        } catch (Exception ignored) {}
    }
}
