package com.snappfood.handler.admin;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.google.gson.Gson;
import com.snappfood.model.Order;
import com.snappfood.repository.OrderRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class ViewAllOrdersHandler implements HttpHandler {
    private static final String APPLICATION_JSON = "application/json";
    private static final String SECRET = "YOUR_SECRET_KEY";
    private static final String BEARER_PREFIX = "Bearer ";

    private final Gson gson = new Gson();
    private final OrderRepository orderRepo = new OrderRepository();
    private final JWTVerifier verifier = JWT.require(Algorithm.HMAC256(SECRET)).build();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        int status;
        Object responseBody;
        try {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                status = 405;
                responseBody = Map.of("error", "Method not allowed");
                sendResponse(exchange, status, responseBody);
                return;
            }

            String auth = exchange.getRequestHeaders().getFirst("Authorization");
            if (auth == null || !auth.startsWith(BEARER_PREFIX)) {
                status = 401;
                responseBody = Map.of("error", "Missing Authorization header");
                sendResponse(exchange, status, responseBody);
                return;
            }

            DecodedJWT jwt;
            try {
                jwt = verifier.verify(auth.substring(BEARER_PREFIX.length()).trim());
            } catch (Exception e) {
                status = 401;
                responseBody = Map.of("error", "Invalid or expired token");
                sendResponse(exchange, status, responseBody);
                return;
            }

            String phone = jwt.getClaim("phone").asString();
            if (!"admin".equals(phone)) {
                status = 403;
                responseBody = Map.of("error", "Admin only");
                sendResponse(exchange, status, responseBody);
                return;
            }

            List<Order> orders = orderRepo.findAll();
            status = 200;
            responseBody = orders;
        } catch (Exception ex) {
            ex.printStackTrace();
            status = 500;
            responseBody = Map.of("error", "Internal server error");
        }
        sendResponse(exchange, status, responseBody);
    }

    private void sendResponse(HttpExchange exchange, int statusCode, Object body) throws IOException {
        byte[] json = gson.toJson(body).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", APPLICATION_JSON + "; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, json.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(json);
        }
    }
}
