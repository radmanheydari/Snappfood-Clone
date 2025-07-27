package com.snappfood.handler.courier;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.snappfood.Role;
import com.snappfood.dto.OrderDTO;
import com.snappfood.model.Order;
import com.snappfood.repository.OrderRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GetAvailableDeliveryRequestsHandler implements HttpHandler {
    private static final String APPLICATION_JSON = "application/json";
    private static final String SECRET           = "YOUR_SECRET_KEY";
    private static final String BEARER_PREFIX    = "Bearer ";

    Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>)
                    (src, typeOfSrc, context) -> new JsonPrimitive(src.toString()))
            .create();
    private final JWTVerifier verifier = JWT.require(Algorithm.HMAC256(SECRET)).build();
    private final OrderRepository orderRepo = new OrderRepository();

    @Override
    public void handle(HttpExchange exchange) {
        try {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, Map.of("error", "Method not allowed"));
                return;
            }

            String auth = exchange.getRequestHeaders().getFirst("Authorization");
            if (auth == null || !auth.startsWith(BEARER_PREFIX)) {
                sendResponse(exchange, 401, Map.of("error", "Missing or invalid Authorization header"));
                return;
            }

            String token = auth.substring(BEARER_PREFIX.length()).trim();
            var jwt   = verifier.verify(token);
            String role = jwt.getClaim("role").asString();
            if (role == null || !Role.COURIER.name().equals(role)) {
                sendResponse(exchange, 403, Map.of("error", "Only couriers may fetch delivery requests"));
                return;
            }

            List<Order> orders = orderRepo.findAvailableForDelivery();

            List<OrderDTO> dtos = orders.stream()
                    .map(OrderDTO::new)
                    .collect(Collectors.toList());

            sendResponse(exchange, 200, dtos);

        } catch (Exception e) {
            e.printStackTrace();
            try {
                sendResponse(exchange, 500, Map.of("error", "Internal server error"));
            } catch (Exception ignored) {}
        }
    }

    private void sendResponse(HttpExchange ex, int status, Object body) throws Exception {
        String json = gson.toJson(body);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", APPLICATION_JSON + ";charset=UTF-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }
}
