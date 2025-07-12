package com.snappfood.handler.buyer;

import com.google.gson.Gson;
import com.snappfood.Role;
import com.snappfood.dto.MenuWithItemsDTO;
import com.snappfood.model.Menu;
import com.snappfood.model.Restaurant;
import com.snappfood.model.User;
import com.snappfood.repository.MenuRepository;
import com.snappfood.repository.RestaurantRepository;
import com.snappfood.repository.UserRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.OutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class GetVendorMenuHandler implements HttpHandler {
    private static final String APPLICATION_JSON = "application/json";
    private final Gson               gson        = new Gson();
    private final UserRepository     userRepo    = new UserRepository();
    private final RestaurantRepository restRepo   = new RestaurantRepository();
    private final MenuRepository     menuRepo    = new MenuRepository();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        byte[] response = new byte[0];
        int    status = 200;
        exchange.getResponseHeaders().set("Content-Type", APPLICATION_JSON);

        try {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                status   = 405;
                response = gson.toJson(Map.of("error","Method not allowed"))
                        .getBytes(StandardCharsets.UTF_8);
                return;
            }

            // Parse vendorId from path /vendors/{vendorId}
            String[] parts = exchange.getRequestURI().getPath().split("/");
            if (parts.length != 3) {
                status   = 400;
                response = gson.toJson(Map.of("error","Bad path")).getBytes(StandardCharsets.UTF_8);
                return;
            }
            long vendorId;
            try {
                vendorId = Long.parseLong(parts[2]);
            } catch (NumberFormatException e) {
                status   = 400;
                response = gson.toJson(Map.of("error","Invalid vendor ID")).getBytes(StandardCharsets.UTF_8);
                return;
            }

            // Verify vendor exists and is a seller
            Optional<User> userOpt = userRepo.findById(vendorId);
            if (userOpt.isEmpty() || userOpt.get().getRole() != Role.SELLER) {
                status   = 404;
                response = gson.toJson(Map.of("error","Vendor not found")).getBytes(StandardCharsets.UTF_8);
                return;
            }

            // Fetch restaurants owned by this vendor
            List<Restaurant> owned = restRepo.findByOwnerId(vendorId);

            // Gather all menus (eagerly load items)
            List<Menu> menus = owned.stream()
                    .flatMap(r -> menuRepo.findAllByRestaurantId(r.getId()).stream())
                    .collect(Collectors.toList());

            List<MenuWithItemsDTO> dtos = menus.stream()
                    .map(MenuWithItemsDTO::new)
                    .collect(Collectors.toList());

            response = gson.toJson(dtos).getBytes(StandardCharsets.UTF_8);

        } catch (Exception e) {
            e.printStackTrace();
            status   = 500;
            response = gson.toJson(Map.of("error","Internal server error"))
                    .getBytes(StandardCharsets.UTF_8);
        } finally {
            exchange.sendResponseHeaders(status, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        }
    }
}
