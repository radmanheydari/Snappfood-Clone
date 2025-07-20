package com.snappfood.handler.buyer;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.google.gson.Gson;
import com.snappfood.Role;
import com.snappfood.model.Food;
import com.snappfood.model.Rating;
import com.snappfood.model.User;
import com.snappfood.repository.FoodRepository;
import com.snappfood.repository.RatingRepository;
import com.snappfood.repository.UserRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

public class SubmitRatingHandler implements HttpHandler {
    private static final String APPLICATION_JSON = "application/json";
    private static final String SECRET = "YOUR_SECRET_KEY";
    private static final String BEARER_PREFIX = "Bearer ";

    private final Gson gson = new Gson();
    private final JWTVerifier verifier = JWT.require(Algorithm.HMAC256(SECRET)).build();
    private final UserRepository userRepo = new UserRepository();
    private final FoodRepository foodRepo = new FoodRepository();
    private final RatingRepository ratingRepo = new RatingRepository();

    static class Request {
        public int rating;
        public String comment;
        public String imageBase64;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        String auth = exchange.getRequestHeaders().getFirst("Authorization");
        if (auth == null || !auth.startsWith(BEARER_PREFIX)) {
            sendJson(exchange, 401, Map.of("error", "Missing Authorization header"));
            return;
        }

        User user;
        try {
            DecodedJWT jwt = verifier.verify(auth.substring(BEARER_PREFIX.length()).trim());
            long userId = Long.parseLong(jwt.getSubject());
            Optional<User> uopt = userRepo.findById(userId);
            if (uopt.isEmpty() || uopt.get().getRole() != Role.BUYER) {
                sendJson(exchange, 403, Map.of("error", "Only buyers can submit ratings"));
                return;
            }
            user = uopt.get();
        } catch (Exception e) {
            sendJson(exchange, 401, Map.of("error", "Invalid token"));
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String[] parts = path.split("/");
        if (parts.length < 4) {
            sendJson(exchange, 400, Map.of("error", "Invalid path"));
            return;
        }

        long itemId;
        try {
            itemId = Long.parseLong(parts[2]);
        } catch (NumberFormatException e) {
            sendJson(exchange, 400, Map.of("error", "Invalid item id"));
            return;
        }

        Optional<Food> foodOpt = foodRepo.findById(itemId);
        if (foodOpt.isEmpty()) {
            sendJson(exchange, 404, Map.of("error", "Food item not found"));
            return;
        }

        Request req;
        try (InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
            req = gson.fromJson(reader, Request.class);
        }

        if (req == null || req.rating < 1 || req.rating > 5 || req.comment == null) {
            sendJson(exchange, 400, Map.of("error", "Invalid rating data"));
            return;
        }

        Rating rating = new Rating();
        rating.setRating(req.rating);
        rating.setComment(req.comment);
        rating.setImageBase64(req.imageBase64);
        rating.setUser(user);
        rating.setFood(foodOpt.get());

        ratingRepo.save(rating);

        sendJson(exchange, 200, Map.of("message", "Rating submitted successfully"));
    }

    private void sendJson(HttpExchange ex, int status, Object body) throws IOException {
        byte[] bytes = gson.toJson(body).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", APPLICATION_JSON);
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }
}
