package com.snappfood.handler.buyer;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.google.gson.*;
import com.snappfood.Role;
import com.snappfood.dto.OrderDTO;
import com.snappfood.model.Order;
import com.snappfood.model.User;
import com.snappfood.repository.OrderRepository;
import com.snappfood.repository.UserRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.OutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

public class GetOrderDetailHandler implements HttpHandler {
    private static final String APPLICATION_JSON = "application/json";
    private static final String SECRET           = "YOUR_SECRET_KEY";
    private static final String BEARER_PREFIX    = "Bearer ";

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, type, ctx) ->
                    new JsonPrimitive(src.toString()))
            .create();

    private final JWTVerifier verifier = JWT.require(Algorithm.HMAC256(SECRET)).build();
    private final UserRepository userRepo = new UserRepository();
    private final OrderRepository orderRepo = new OrderRepository();
    private final long orderId;

    public GetOrderDetailHandler(long orderId) {
        this.orderId = orderId;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        int status = 200;
        byte[] responseBytes = new byte[0];
        exchange.getResponseHeaders().set("Content-Type", APPLICATION_JSON);

        try {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                status = 405;
                responseBytes = gson.toJson(Map.of("error","Method not allowed"))
                        .getBytes(StandardCharsets.UTF_8);
                return;
            }

            String auth = exchange.getRequestHeaders().getFirst("Authorization");
            if (auth == null || !auth.startsWith(BEARER_PREFIX)) {
                status = 401;
                responseBytes = gson.toJson(Map.of("error","Missing Authorization header"))
                        .getBytes(StandardCharsets.UTF_8);
                return;
            }

            DecodedJWT jwt;
            try {
                jwt = verifier.verify(auth.substring(BEARER_PREFIX.length()).trim());
            } catch (Exception e) {
                status = 401;
                responseBytes = gson.toJson(Map.of("error","Invalid or expired token"))
                        .getBytes(StandardCharsets.UTF_8);
                return;
            }

            long userId = Long.parseLong(jwt.getSubject());
            Optional<User> uopt = userRepo.findById(userId);
            if (uopt.isEmpty()) {
                status = 401;
                responseBytes = gson.toJson(Map.of("error","Invalid token"))
                        .getBytes(StandardCharsets.UTF_8);
                return;
            }
            User current = uopt.get();

            Optional<Order> oopt = orderRepo.findById(orderId);
            if (oopt.isEmpty()) {
                status = 404;
                responseBytes = gson.toJson(Map.of("error","Order not found"))
                        .getBytes(StandardCharsets.UTF_8);
                return;
            }
            Order order = oopt.get();

            long uid = current.getId();
            boolean allowed =
                    (current.getRole() == Role.BUYER   && order.getCustomer().getId().equals(uid)) ||
                            (current.getRole() == Role.SELLER  && order.getVendor().getId().equals(uid)) ||
                            (order.getCourier() != null        && order.getCourier().getId().equals(uid));

            if (!allowed) {
                status = 403;
                responseBytes = gson.toJson(Map.of("error","Forbidden"))
                        .getBytes(StandardCharsets.UTF_8);
                return;
            }

            OrderDTO dto = new OrderDTO(order);
            responseBytes = gson.toJson(dto).getBytes(StandardCharsets.UTF_8);

        } catch (Exception e) {
            e.printStackTrace();
            status = 500;
            responseBytes = gson.toJson(Map.of("error","Internal server error"))
                    .getBytes(StandardCharsets.UTF_8);
        } finally {
            exchange.sendResponseHeaders(status, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        }
    }
}
