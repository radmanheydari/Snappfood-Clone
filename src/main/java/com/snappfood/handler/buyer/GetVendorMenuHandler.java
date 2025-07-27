package com.snappfood.handler.buyer;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.google.gson.Gson;
import com.snappfood.Role;
import com.snappfood.model.Food;
import com.snappfood.model.Menu;
import com.snappfood.model.Restaurant;
import com.snappfood.dto.RestaurantDTO;
import com.snappfood.dto.FoodItemDTO;
import com.snappfood.repository.MenuRepository;
import com.snappfood.repository.RestaurantRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class GetVendorMenuHandler implements HttpHandler {
    private static final String APPLICATION_JSON = "application/json";
    private static final String SECRET           = "YOUR_SECRET_KEY";
    private static final String BEARER_PREFIX    = "Bearer ";

    private final RestaurantRepository restaurantRepo = new RestaurantRepository();
    private final MenuRepository       menuRepo       = new MenuRepository();
    private final JWTVerifier          verifier       = JWT.require(Algorithm.HMAC256(SECRET)).build();
    private final Gson                 gson           = new Gson();

    @Override
    public void handle(HttpExchange exchange) {
        try {
            // only GET
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                send(exchange, 405, Map.of("error", "Method not allowed"));
                return;
            }

            // auth header
//            String auth = exchange.getRequestHeaders().getFirst("Authorization");
//            if (auth == null || !auth.startsWith(BEARER_PREFIX)) {
//                send(exchange, 401, Map.of("error", "Missing or invalid Authorization header"));
//                return;
//            }
//
//            // verify token & role
//            DecodedJWT jwt = verifier.verify(auth.substring(BEARER_PREFIX.length()).trim());
//            if (!Role.BUYER.name().equals(jwt.getClaim("role").asString())) {
//                send(exchange, 403, Map.of("error", "Only buyers may view vendor menus"));
//                return;
//            }

            // extract vendor id
            String[] segments = exchange.getRequestURI().getPath().split("/");
            long vendorId = Long.parseLong(segments[2]);

            // load restaurant
            Optional<Restaurant> restOpt = restaurantRepo.findById(vendorId);
            if (restOpt.isEmpty()) {
                send(exchange, 404, Map.of("error", "Vendor not found"));
                return;
            }
            Restaurant rest = restOpt.get();
            RestaurantDTO vendorDto = new RestaurantDTO(rest);

            // load menus
            List<Menu> menus = menuRepo.findByRestaurantId(vendorId);
            List<String> titles = new ArrayList<>();
            Map<String, List<FoodItemDTO>> byTitle = new LinkedHashMap<>();

            for (Menu m : menus) {
                String title = m.getTitle();
                titles.add(title);
                List<FoodItemDTO> items = new ArrayList<>();
                for (Food f : m.getFoodItems()) {
                    items.add(new FoodItemDTO(f));
                }
                byTitle.put(title, items);
            }

            // build response
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("vendor", vendorDto);
            resp.put("menu_titles", titles);
            // then each title key
            for (var entry : byTitle.entrySet()) {
                resp.put(entry.getKey(), entry.getValue());
            }

            send(exchange, 200, resp);

        } catch (com.auth0.jwt.exceptions.JWTVerificationException e) {
            send(exchange, 401, Map.of("error", "Invalid or expired token"));
        } catch (NumberFormatException e) {
            send(exchange, 400, Map.of("error", "Invalid vendor ID"));
        } catch (Exception e) {
            e.printStackTrace();
            send(exchange, 500, Map.of("error", "Internal server error"));
        }
    }

    private void send(HttpExchange ex, int status, Object body) {
        try {
            byte[] bytes = gson.toJson(body).getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().set("Content-Type", APPLICATION_JSON + "; charset=UTF-8");
            ex.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(bytes);
            }
        } catch (Exception ignored) {}
    }
}
