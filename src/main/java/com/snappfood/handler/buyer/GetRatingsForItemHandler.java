package com.snappfood.handler.buyer;

import com.google.gson.*;
import com.snappfood.dto.RatingDTO;
import com.snappfood.model.Rating;
import com.snappfood.repository.RatingRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

public class GetRatingsForItemHandler implements HttpHandler {
    private static final String APPLICATION_JSON = "application/json";
    private final RatingRepository ratingRepo = new RatingRepository();
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new JsonDeserializer<LocalDateTime>() {
                @Override
                public LocalDateTime deserialize(JsonElement json, Type type, JsonDeserializationContext ctx) throws JsonParseException {
                    return LocalDateTime.parse(json.getAsString());
                }
            })
            .registerTypeAdapter(LocalDateTime.class, new JsonSerializer<LocalDateTime>() {
                @Override
                public JsonElement serialize(LocalDateTime dateTime, Type type, JsonSerializationContext ctx) {
                    return new JsonPrimitive(dateTime.toString());
                }
            })
            .create();


    @Override
    public void handle(HttpExchange exchange) {
        try {
            String path = exchange.getRequestURI().getPath();
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String[] parts = path.split("/");
            long itemId = Long.parseLong(parts[2]);

            List<Rating> ratings = ratingRepo.findByFoodId(itemId);
            List<RatingDTO> dtoList = ratings.stream()
                    .map(RatingDTO::new)
                    .toList();
            String json = gson.toJson(dtoList);

            exchange.getResponseHeaders().set("Content-Type", APPLICATION_JSON);
            exchange.sendResponseHeaders(200, json.getBytes(StandardCharsets.UTF_8).length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                exchange.sendResponseHeaders(500, -1);
            } catch (Exception ignored) {}
        }
    }
}
