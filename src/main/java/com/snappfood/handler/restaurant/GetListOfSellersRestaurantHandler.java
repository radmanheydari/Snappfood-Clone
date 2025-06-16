//package com.snappfood.handler.restaurant;
//
//import com.auth0.jwt.JWT;
//import com.auth0.jwt.algorithms.Algorithm;
//import com.auth0.jwt.exceptions.JWTVerificationException;
//import com.auth0.jwt.interfaces.DecodedJWT;
//import com.auth0.jwt.interfaces.JWTVerifier;
//import com.google.gson.Gson;
//import com.snappfood.Role;
//import com.snappfood.dto.RestaurantDTO;
//import com.snappfood.model.Restaurant;
//import com.snappfood.model.User;
//import com.snappfood.repository.UserRepository;
//import com.sun.net.httpserver.HttpExchange;
//import com.sun.net.httpserver.HttpHandler;
//
//import java.io.IOException;
//import java.io.OutputStream;
//import java.nio.charset.StandardCharsets;
//import java.util.*;
//
//public class GetListOfSellersRestaurantHandler implements HttpHandler {
//    private static final String APPLICATION_JSON = "application/json";
//    private static final String SECRET = "YOUR_SECRET_KEY";
//    private static final String BEARER_PREFIX = "Bearer ";
//    private final Gson gson = new Gson();
//    private final JWTVerifier verifier = JWT.require(Algorithm.HMAC256(SECRET)).build();
//    private final UserRepository userRepository = new UserRepository();
//
//    @Override
//    public void handle(HttpExchange exchange) throws IOException {
//        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
//            sendJsonResponse(exchange, 405, errorJson("Method not allowed"));
//            return;
//        }
//
//        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
//        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
//            sendJsonResponse(exchange, 401, errorJson("Missing or invalid Authorization header"));
//            return;
//        }
//
//        String token = authHeader.substring(BEARER_PREFIX.length()).trim();
//        DecodedJWT jwt;
//        try {
//            jwt = verifier.verify(token);
//        } catch (JWTVerificationException e) {
//            sendJsonResponse(exchange, 401, errorJson("Invalid or expired token"));
//            return;
//        }
//
//        String sub = jwt.getSubject();
//        if (sub == null) {
//            sendJsonResponse(exchange, 401, errorJson("Invalid token payload"));
//            return;
//        }
//
//        User user;
//        try {
//            long userId = Long.parseLong(sub);
//            Optional<User> optionalUser = userRepository.findById(userId);
//            if (optionalUser.isEmpty()) {
//                sendJsonResponse(exchange, 404, errorJson("User not found"));
//                return;
//            }
//            user = optionalUser.get();
//        } catch (Exception e) {
//            sendJsonResponse(exchange, 400, errorJson("Invalid user ID in token"));
//            return;
//        }
//
//        if (user.getRole() != Role.SELLER) {
//            sendJsonResponse(exchange, 403, errorJson("Only sellers can view their restaurants"));
//            return;
//        }
//
//        Set<Restaurant> owned = user.getOwnedRestaurants();
//        List<RestaurantDTO> dtos = new ArrayList<>();
//        if (owned != null) {
//            for (Restaurant r : owned) {
//                dtos.add(new RestaurantDTO(r));
//            }
//        }
//
//        Map<String, Object> response = new HashMap<>();
//        response.put("message", "Seller's restaurants fetched successfully");
//        response.put("restaurants", dtos);
//        String json = gson.toJson(response);
//        sendJsonResponse(exchange, 200, json);
//    }
//
//    private String errorJson(String message) {
//        return gson.toJson(Collections.singletonMap("error", message));
//    }
//
//    private void sendJsonResponse(HttpExchange exchange, int statusCode, String json) throws IOException {
//        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
//        exchange.getResponseHeaders().set("Content-Type", APPLICATION_JSON);
//        exchange.sendResponseHeaders(statusCode, bytes.length);
//        try (OutputStream os = exchange.getResponseBody()) {
//            os.write(bytes);
//        }
//    }
//}
