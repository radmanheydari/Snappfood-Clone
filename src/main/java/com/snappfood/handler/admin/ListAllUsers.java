package com.snappfood.handler.admin;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.google.gson.Gson;
import com.snappfood.model.User;
import com.snappfood.repository.UserRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.OutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class ListAllUsers implements HttpHandler {
    private static final String APPLICATION_JSON = "application/json";
    private static final String SECRET = "YOUR_SECRET_KEY";
    private static final String BEARER_PREFIX = "Bearer ";

    private final Gson gson = new Gson();
    private final JWTVerifier verifier = JWT.require(Algorithm.HMAC256(SECRET)).build();
    private final UserRepository userRepo = new UserRepository();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        int status = 200;
        Object body = null;

        try {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                status = 405;
                body   = Map.of("error", "Method not allowed");
                return;
            }

            String auth = exchange.getRequestHeaders().getFirst("Authorization");
            if (auth == null || !auth.startsWith(BEARER_PREFIX)) {
                status = 401;
                body   = Map.of("error", "Missing Authorization header");
                return;
            }

            DecodedJWT jwt = verifier.verify(auth.substring(BEARER_PREFIX.length()).trim());
            String phone = jwt.getClaim("phone").asString();

            //debugging
            System.out.println("Token Claims:");
            jwt.getClaims().forEach((key, val) -> System.out.println(key + " = " + val.asString()));

            if (!"admin".equals(phone)) {
                status = 403;
                body   = Map.of("error", "Forbidden: Admins only");
                return;
            }

            List<User> users = userRepo.findAll();
            body = users;

        } catch (Exception e) {
            e.printStackTrace();
            status = 500;
            body   = Map.of("error", "Internal server error");
        } finally {
            byte[] bytes = gson.toJson(body).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", APPLICATION_JSON);
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }
}
