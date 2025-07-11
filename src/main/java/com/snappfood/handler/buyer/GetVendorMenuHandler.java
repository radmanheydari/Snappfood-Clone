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
    private final Gson gson  = new Gson();
    private final UserRepository userRepo = new UserRepository();
    private final RestaurantRepository restRepo = new RestaurantRepository();
    private final MenuRepository menuRepo = new MenuRepository();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        if (!"GET".equalsIgnoreCase(method)) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }


        String[] parts = exchange.getRequestURI().getPath().split("/");
        if (parts.length != 3) {
            exchange.sendResponseHeaders(400, -1);
            return;
        }
        long vendorId;
        try {
            vendorId = Long.parseLong(parts[2]);
        } catch (NumberFormatException e) {
            exchange.sendResponseHeaders(400, -1);
            return;
        }


        Optional<User> userOpt = userRepo.findById(vendorId);
        if (userOpt.isEmpty() || userOpt.get().getRole() != Role.SELLER) {
            exchange.sendResponseHeaders(404, -1);
            return;
        }


        List<Restaurant> owned = restRepo.findByOwnerId(vendorId);
        if (owned.isEmpty()) {

            byte[] resp = "[]".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", APPLICATION_JSON);
            exchange.sendResponseHeaders(200, resp.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp);
            }
            return;
        }


        List<Menu> menus = owned.stream()
                .flatMap(r -> menuRepo.findAllByRestaurantId(r.getId()).stream())
                .collect(Collectors.toList());

        //FIXME : SOCKET HANG UP ERROR WHEN MENU IS NOT EMPTY
        List<MenuWithItemsDTO> dtos = menus.stream()
                .map(MenuWithItemsDTO::new)
                .collect(Collectors.toList());

        byte[] resp = gson.toJson(dtos).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", APPLICATION_JSON);
        exchange.sendResponseHeaders(200, resp.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(resp);
        }
    }
}
