package com.snappfood;

import com.google.gson.Gson;
import com.snappfood.handler.restaurant.CreateNewRestaurantHandler;
import com.snappfood.handler.restaurant.GetListOfSellersRestaurantHandler;
import com.snappfood.handler.restaurant.UpdateRestaurantHandler;
import com.snappfood.handler.user.CurrentUserHandler;
import com.snappfood.handler.user.LoginHandler;
import com.snappfood.handler.user.LogoutHandler;
import com.snappfood.handler.user.RegisterHandler;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class Main {
    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // User-related routes
        server.createContext("/auth/register", new RegisterHandler());
        server.createContext("/auth/login", new LoginHandler());
        server.createContext("/auth/profile", new CurrentUserHandler());
        server.createContext("/auth/logout", new LogoutHandler());

        // Restaurant routes
        server.createContext("/restaurants", new RestaurantRouter());

        server.createContext("/restaurants/mine", new GetListOfSellersRestaurantHandler());

        server.start();
        System.out.println("Server running on port 8080");
    }

    // Router for /restaurants and /restaurants/{id}
    static class RestaurantRouter implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath(); // e.g., /restaurants or /restaurants/1

            if (path.equals("/restaurants") && method.equalsIgnoreCase("POST")) {
                new CreateNewRestaurantHandler().handle(exchange);
                return;
            }

            // Handle PUT /restaurants/{id}
            if (method.equalsIgnoreCase("PUT") && path.matches("^/restaurants/\\d+$")) {
                try {
                    String[] parts = path.split("/");
                    long restaurantId = Long.parseLong(parts[2]);
                    new UpdateRestaurantHandler(restaurantId).handle(exchange);
                    return;
                } catch (NumberFormatException e) {
                    sendJson(exchange, 400, "{\"error\":\"Invalid restaurant ID\"}");
                    return;
                }
            }

            sendJson(exchange, 404, "{\"error\":\"Not found\"}");
        }

        private void sendJson(HttpExchange exchange, int statusCode, String json) throws IOException {
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(statusCode, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }
}
