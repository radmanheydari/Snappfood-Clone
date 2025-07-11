package com.snappfood.handler.buyer;

import com.google.gson.Gson;
import com.snappfood.dto.RestaurantDTO;
import com.snappfood.model.Restaurant;
import com.snappfood.repository.RestaurantRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.OutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class ListVendorsHandler implements HttpHandler {
    private static final String APPLICATION_JSON = "application/json";
    private final Gson gson = new Gson();
    private final RestaurantRepository repo = new RestaurantRepository();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        URI uri = exchange.getRequestURI();
        Map<String, String> params = queryToMap(uri.getRawQuery());
        String name = params.get("name");
        String address = params.get("address");
        String phone = params.get("phone");

        try {
            List<Restaurant> list = repo.findAll(name, address, phone);
            List<RestaurantDTO> dtos = list.stream()
                    .map(RestaurantDTO::new)
                    .collect(Collectors.toList());
            byte[] resp = gson.toJson(dtos).getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders().set("Content-Type", APPLICATION_JSON);
            exchange.sendResponseHeaders(200, resp.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp);
            }
        } catch (Exception e) {
            e.printStackTrace();
            byte[] err = gson.toJson(Map.of("error", "Internal server error"))
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", APPLICATION_JSON);
            exchange.sendResponseHeaders(500, err.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(err);
            }
        }
    }

    private static Map<String, String> queryToMap(String query) {
        if (query == null || query.isBlank()) return Collections.emptyMap();
        Map<String, String> map = new HashMap<>();
        for (String kv : query.split("&")) {
            String[] parts = kv.split("=", 2);
            if (parts.length == 2) {
                map.put(parts[0], parts[1]);
            }
        }
        return map;
    }
}
