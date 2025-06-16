package com.snappfood.handler.user;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.snappfood.dto.LoginDTO;
import com.snappfood.dto.LoginDTO.UserData;
import com.snappfood.model.User;
import com.snappfood.repository.UserRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

public class LoginHandler implements HttpHandler {
    private static final String APPLICATION_JSON = "application/json";
    private static final String SECRET = "YOUR_SECRET_KEY"; // ← حتماً یک مقدار قوی قرار دهید
    private static final long   EXPIRATION_MS = 1000L * 60 * 60; // 1 ساعت

    private final Gson gson = new Gson();
    private final UserRepository userRepository = new UserRepository();
    private final Algorithm algorithm = Algorithm.HMAC256(SECRET);

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            // تنها POST پذیرفته می‌شود
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendRawResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            // 1. خواندن JSON از بدنه‌ی درخواست
            InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
            Type mapType = new TypeToken<Map<String, String>>() {}.getType();
            Map<String, String> userData = gson.fromJson(reader, mapType);

            // اگر JSON خالی یا نامعتبر است، Bad Request می‌دهیم
            if (userData == null) {
                sendRawResponse(exchange, 400, "{\"error\":\"Request body is missing or invalid JSON\"}");
                return;
            }

            String phone = userData.get("phone");
            String password = userData.get("password");

            // چک کردن فیلدهای ضروری
            if (phone == null || password == null) {
                sendRawResponse(exchange, 400, "{\"error\":\"Both 'phone' and 'password' fields are required\"}");
                return;
            }

            // 2. پیدا کردن کاربر با شماره تلفن
            Optional<User> optionalUser = userRepository.findByPhone(phone);
            if (optionalUser.isEmpty()) {
                sendRawResponse(exchange, 401, "{\"error\":\"Invalid credentials\"}");
                return;
            }

            User user = optionalUser.get();

            // 3. بررسی درست بودن پسورد
            // اگر پسورد در دیتابیس هش نشده، به این شکل مستقیم مقایسه کنید:
            if (!user.getPassword().equals(password)) {
                sendRawResponse(exchange, 401, "{\"error\":\"Invalid credentials\"}");
                return;
            }
            // اگر پسورد در DB هش شده است، از BCrypt (یا الگوریتم مناسب) استفاده کنید:
            // if (!BCrypt.checkpw(password, user.getPassword())) { ... }

            // 4. ساختن JWT
            Date now = new Date();
            Date expiresAt = new Date(now.getTime() + EXPIRATION_MS);
            String token = JWT.create()
                    .withSubject(String.valueOf(user.getId()))
                    .withClaim("role", user.getRole().toString())
                    .withIssuedAt(now)
                    .withExpiresAt(expiresAt)
                    .sign(algorithm);

            // 5. آماده‌سازی DTO برای پاسخ
            UserData dtoUser = new UserData(user);
            LoginDTO responseDto = new LoginDTO(
                    "user logged in successfully",
                    token,
                    dtoUser
            );

            String jsonResponse = gson.toJson(responseDto);

            // ارسال پاسخ 200
            sendJsonResponse(exchange, 200, jsonResponse);

        } catch (Exception e) {
            // اگر هر استثنائی رخ داد، آن را چاپ کرده و 500 برمی‌گردانیم
            e.printStackTrace();
            sendRawResponse(exchange, 500, "{\"error\":\"Internal server error\"}");
        }
    }

    // ارسال پاسخ JSON (200 یا هر کد دیگر) با هدر Content-Type مناسب
    private void sendJsonResponse(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", APPLICATION_JSON + "; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    // ارسال یک متن ساده (غالباً JSON خطا) بدون header Content-Type پیچیده
    private void sendRawResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", APPLICATION_JSON + "; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
