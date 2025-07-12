package com.snappfood.handler.buyer;

import com.google.gson.Gson;
import com.snappfood.dto.ItemDTO;
import com.snappfood.model.Food;
import com.snappfood.repository.FoodRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.OutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

public class GetItemDetailsHandler implements HttpHandler {
    private static final String APPLICATION_JSON = "application/json";
    private final Gson          gson = new Gson();
    private final FoodRepository repo = new FoodRepository();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        // Parse path /items/{id}
        String path = exchange.getRequestURI().getPath();
        String[] parts = path.split("/");
        if (parts.length != 3) {
            sendJson(exchange, 400, Map.of("error", "Bad path"));
            return;
        }

        long itemId;
        try {
            itemId = Long.parseLong(parts[2]);
        } catch (NumberFormatException e) {
            sendJson(exchange, 400, Map.of("error", "Invalid item ID"));
            return;
        }

        // Fetch item
        Optional<Food> opt = repo.findById(itemId);
        if (opt.isEmpty()) {
            sendJson(exchange, 404, Map.of("error", "Item not found"));
            return;
        }

        // Build DTO and respond
        ItemDTO dto = new ItemDTO(opt.get());
        sendJson(exchange, 200, dto);
    }

    private void sendJson(HttpExchange exchange, int statusCode, Object data) throws IOException {
        byte[] bytes = gson.toJson(data).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", APPLICATION_JSON);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
