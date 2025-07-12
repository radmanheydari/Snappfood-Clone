package com.snappfood.handler.buyer;

import com.google.gson.Gson;
import com.snappfood.dto.ItemDTO;
import com.snappfood.model.Food;
import com.snappfood.repository.FoodRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.OutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class ListItemHandler implements HttpHandler {
    private static final String APPLICATION_JSON = "application/json";
    private final Gson          gson = new Gson();
    private final FoodRepository repo = new FoodRepository();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        // parse ?name=&min_price=&max_price=&category_id=
        URI uri = exchange.getRequestURI();
        Map<String, String> params = queryToMap(uri.getRawQuery());
        String  name       = params.get("name");
        Integer minPrice   = params.containsKey("min_price") ? parseInt(params.get("min_price")) : null;
        Integer maxPrice   = params.containsKey("max_price") ? parseInt(params.get("max_price")) : null;
        Long    categoryId = params.containsKey("category_id") ? parseLong(params.get("category_id")) : null;

        try {
            List<Food> items = repo.findAllWithFilters(name, minPrice, maxPrice, categoryId);
            List<ItemDTO> dtos = items.stream()
                    .map(ItemDTO::new)
                    .collect(Collectors.toList());
            byte[] resp = gson.toJson(dtos).getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders().set("Content-Type", APPLICATION_JSON);
            exchange.sendResponseHeaders(200, resp.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp);
            }
        } catch (Exception e) {
            e.printStackTrace();
            byte[] err = gson.toJson(Map.of("error","Internal server error"))
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", APPLICATION_JSON);
            exchange.sendResponseHeaders(500, err.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(err);
            }
        }
    }

    private static Map<String,String> queryToMap(String query) {
        if (query==null||query.isBlank()) return Collections.emptyMap();
        Map<String,String> m = new HashMap<>();
        for (String kv: query.split("&")) {
            String[] parts = kv.split("=",2);
            if (parts.length==2) m.put(parts[0], parts[1]);
        }
        return m;
    }

    private static Integer parseInt(String s) {
        try { return Integer.parseInt(s); } catch(Exception e){ return null; }
    }
    private static Long parseLong(String s) {
        try { return Long.parseLong(s); } catch(Exception e){ return null; }
    }
}
