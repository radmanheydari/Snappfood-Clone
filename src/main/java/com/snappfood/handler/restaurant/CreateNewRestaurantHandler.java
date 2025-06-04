package com.snappfood.handler.restaurant;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.snappfood.model.Restaurant;
import com.snappfood.repository.RestaurantRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

public class CreateNewRestaurantHandler implements HttpHandler {
    private final Gson gson = new Gson();
    private final RestaurantRepository restaurantRepository = new RestaurantRepository();

    @Override
    public void handle(HttpExchange exchange) {
        try {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, errorJson("Method not allowed"));
                return;
            }

            InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
            Type type = new TypeToken<Map<String, Object>>() {}.getType();
            Map<String, Object> data = gson.fromJson(reader, type);

            if (data == null || !data.containsKey("name") || !data.containsKey("address") || !data.containsKey("phone")) {
                sendJsonResponse(exchange, 400, errorJson("Missing required fields"));
                return;
            }

            Restaurant restaurant = new Restaurant();
            restaurant.setName((String) data.get("name"));
            restaurant.setAddress((String) data.get("address"));
            restaurant.setPhone((String) data.get("phone"));

            if (data.containsKey("logoBase64")) {
                restaurant.setLogoBase64((String) data.get("logoBase64"));
            }
            if (data.containsKey("tax_fee")) {
                restaurant.setTax_fee(((Number) data.get("tax_fee")).intValue());
            }
            if (data.containsKey("additional_fee")) {
                restaurant.setAdditional_fee(((Number) data.get("additional_fee")).intValue());
            }

            restaurantRepository.save(restaurant);

            sendJsonResponse(exchange, 201, gson.toJson(Collections.singletonMap("message", "Restaurant created successfully")));
        } catch (Exception e) {
            e.printStackTrace();
            try {
                sendJsonResponse(exchange, 500, errorJson("Internal server error"));
            } catch (Exception ignored) {}
        }
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, String json) throws Exception {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String errorJson(String message) {
        return gson.toJson(Collections.singletonMap("error", message));
    }
}
