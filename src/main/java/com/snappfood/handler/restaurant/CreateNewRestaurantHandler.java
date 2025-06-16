package com.snappfood.handler.restaurant;
//FIXME : THERE'S SOMETHING WRONG I SHOULD REWRITE IT
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.snappfood.Role;
import com.snappfood.model.Restaurant;
import com.snappfood.model.User;
import com.snappfood.repository.RestaurantRepository;
import com.snappfood.repository.UserRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public class CreateNewRestaurantHandler implements HttpHandler {
    private static final String APPLICATION_JSON = "application/json";
    private static final String SECRET = "YOUR_SECRET_KEY"; // ← حتماً یک مقدار واقعی قرار دهید
    private static final String BEARER_PREFIX = "Bearer ";

    private final Gson gson = new Gson();
    private final JWTVerifier verifier = JWT.require(Algorithm.HMAC256(SECRET)).build();
    private final UserRepository userRepository = new UserRepository();
    private final RestaurantRepository restaurantRepository = new RestaurantRepository();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // فقط متد POST پذیرفته می‌شود
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJsonResponse(exchange, 405, errorJson("Method not allowed"));
            return;
        }

        // 1. خواندن هدر Authorization و بررسی Token
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            sendJsonResponse(exchange, 401, errorJson("Missing or invalid Authorization header"));
            return;
        }
        String token = authHeader.substring(BEARER_PREFIX.length()).trim();

        DecodedJWT jwt;
        try {
            jwt = verifier.verify(token);
        } catch (JWTVerificationException e) {
            sendJsonResponse(exchange, 401, errorJson("Invalid or expired token"));
            return;
        }

        // 2. استخراج userId از Subject توکن
        String sub = jwt.getSubject();
        if (sub == null) {
            sendJsonResponse(exchange, 401, errorJson("Invalid token payload"));
            return;
        }

        User user;
        try {
            long userId = Long.parseLong(sub);
            Optional<User> optionalUser = userRepository.findById(userId);
            if (optionalUser.isEmpty()) {
                sendJsonResponse(exchange, 404, errorJson("User not found"));
                return;
            }
            user = optionalUser.get();
        } catch (NumberFormatException e) {
            sendJsonResponse(exchange, 400, errorJson("Invalid user ID in token"));
            return;
        } catch (Exception e) {
            sendJsonResponse(exchange, 500, errorJson("Internal server error"));
            return;
        }

        // 3. فقط SELLER اجازهٔ ایجاد رستوران دارد
        if (user.getRole() != Role.SELLER) {
            sendJsonResponse(exchange, 403, errorJson("Only sellers can create restaurants"));
            return;
        }

        // 4. خواندن بدنهٔ JSON request
        try (InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
            Type type = new TypeToken<Map<String, Object>>() {}.getType();
            Map<String, Object> data = gson.fromJson(reader, type);

            // بررسی فیلدهای ضروری
            if (data == null
                    || !data.containsKey("name")
                    || !data.containsKey("address")
                    || !data.containsKey("phone")) {
                sendJsonResponse(exchange, 400, errorJson("Missing required fields"));
                return;
            }

            // ۵. ساخت شیءٔ Restaurant و مقداردهی فیلدها
            Restaurant restaurant = new Restaurant();
            restaurant.setName((String) data.get("name"));
            restaurant.setAddress((String) data.get("address"));
            restaurant.setPhone((String) data.get("phone"));
            // لوگو اختیاری است
            if (data.containsKey("logoBase64")) {
                restaurant.setLogoBase64((String) data.get("logoBase64"));
            }
            // tax_fee و additional_fee باید اعداد صحیح باشند
            try {
                int taxFee = ((Number) data.get("tax_fee")).intValue();
                int addFee = ((Number) data.get("additional_fee")).intValue();
                restaurant.setTax_fee(taxFee);
                restaurant.setAdditional_fee(addFee);
            } catch (ClassCastException cce) {
                sendJsonResponse(exchange, 400, errorJson("Fields 'tax_fee' and 'additional_fee' must be integers"));
                return;
            }

            // ** مهم: قرار دادن مالک (owner) از نوع User **
            restaurant.setOwner(user);

            // ۶. ذخیره در دیتابیس
            restaurantRepository.save(restaurant);

            sendJsonResponse(exchange, 201, gson.toJson(Collections.singletonMap("message", "Restaurant created successfully")));
        } catch (Exception e) {
            e.printStackTrace();
            sendJsonResponse(exchange, 500, errorJson("Internal server error"));
        }
    }

    private String errorJson(String message) {
        return gson.toJson(Collections.singletonMap("error", message));
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", APPLICATION_JSON);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
