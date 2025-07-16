package com.snappfood.handler.buyer;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.google.gson.Gson;
import com.snappfood.Role;
import com.snappfood.dto.RestaurantDTO;
import com.snappfood.model.Restaurant;
import com.snappfood.model.User;
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

public class GetFavoritesHandler implements HttpHandler {
    private static final String APPLICATION_JSON = "application/json";
    private static final String SECRET = "YOUR_SECRET_KEY";
    private static final String BEARER_PREFIX = "Bearer ";

    private final Gson gson = new Gson();
    private final JWTVerifier verifier = JWT.require(Algorithm.HMAC256(SECRET)).build();
    private final UserRepository userRepo = new UserRepository();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        byte[] responseBytes = null;
        int    status = 200;
        exchange.getResponseHeaders().set("Content-Type", APPLICATION_JSON);

        try {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                status = 405;
                responseBytes = gson.toJson(Map.of("error","Method not allowed"))
                        .getBytes(StandardCharsets.UTF_8);
            } else {
                String auth = exchange.getRequestHeaders().getFirst("Authorization");
                if (auth == null || !auth.startsWith(BEARER_PREFIX)) {
                    status = 401;
                    responseBytes = gson.toJson(Map.of("error","Missing Authorization header"))
                            .getBytes(StandardCharsets.UTF_8);
                } else {
                    DecodedJWT jwt;
                    try {
                        jwt = verifier.verify(auth.substring(BEARER_PREFIX.length()).trim());
                    } catch (Exception e) {
                        status = 401;
                        responseBytes = gson.toJson(Map.of("error","Invalid or expired token"))
                                .getBytes(StandardCharsets.UTF_8);
                        jwt = null;
                    }
                    if (jwt != null) {
                        long userId = Long.parseLong(jwt.getSubject());
                        Optional<User> uopt = userRepo.findWithFavorites(userId);
                        if (uopt.isEmpty() || uopt.get().getRole() != Role.BUYER) {
                            status = 403;
                            responseBytes = gson.toJson(Map.of("error","Forbidden"))
                                    .getBytes(StandardCharsets.UTF_8);
                        } else {
                            List<Restaurant> favs = List.copyOf(uopt.get().getFavoriteRestaurants());
                            var dtos = favs.stream()
                                    .map(RestaurantDTO::new)
                                    .collect(Collectors.toList());
                            responseBytes = gson.toJson(dtos)
                                    .getBytes(StandardCharsets.UTF_8);
                        }
                    }
                }
            }
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
