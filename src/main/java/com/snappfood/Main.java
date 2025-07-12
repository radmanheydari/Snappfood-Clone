package com.snappfood;

import com.google.gson.Gson;;
import com.snappfood.handler.buyer.GetVendorMenuHandler;
import com.snappfood.handler.buyer.ListItemHandler;
import com.snappfood.handler.buyer.ListVendorsHandler;
import com.snappfood.handler.restaurant.*;
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

        server.createContext("/auth/register", new RegisterHandler());
        server.createContext("/auth/login", new LoginHandler());
        server.createContext("/auth/profile", new CurrentUserHandler());
        server.createContext("/auth/logout", new LogoutHandler());
        server.createContext("/restaurants", new RestaurantRouter());
        server.createContext("/restaurants/mine", new GetListOfSellersRestaurantHandler());
        server.createContext("/restaurants/mine", new GetListOfSellersRestaurantHandler());
        server.createContext("/vendors", new VendorRouter());
        server.createContext("/items", new ListItemHandler());


        server.start();
        System.out.println("Server running on port 8080");
    }

    static class RestaurantRouter implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            if (path.equals("/restaurants") && method.equalsIgnoreCase("POST")) {
                new CreateNewRestaurantHandler().handle(exchange);
                return;
            }

            if (method.equalsIgnoreCase("POST") && path.matches("^/restaurants/\\d+/item$")) {
                try {
                    String[] parts = path.split("/");
                    long restaurantId = Long.parseLong(parts[2]);
                    new AddFoodItemHandler(restaurantId).handle(exchange);
                    return;
                } catch (NumberFormatException e) {
                    sendJson(exchange, 400, "{\"error\":\"Invalid restaurant ID\"}");
                    return;
                }
            }

            if (method.equalsIgnoreCase("POST") && path.matches("^/restaurants/\\d+/foods$")) {
                try {
                    String[] parts = path.split("/");
                    long restaurantId = Long.parseLong(parts[2]);
                    new AddFoodItemHandler(restaurantId).handle(exchange);
                    return;
                } catch (NumberFormatException e) {
                    sendJson(exchange, 400, "{\"error\":\"Invalid restaurant ID\"}");
                    return;
                }
            }

            if (method.equalsIgnoreCase("PUT") && path.matches("^/restaurants/\\d+/item/\\d+$")) {
                try {
                    String[] parts = path.split("/");
                    long restaurantId = Long.parseLong(parts[2]);
                    long itemId = Long.parseLong(parts[4]);
                    new EditFoodItemHandler(restaurantId, itemId).handle(exchange);
                    return;
                } catch (NumberFormatException e) {
                    sendJson(exchange, 400, "{\"error\":\"Invalid restaurant or item ID\"}");
                    return;
                }
            }

            if (method.equalsIgnoreCase("DELETE") && path.matches("^/restaurants/\\d+/item/\\d+$")) {
                try {
                    String[] parts = path.split("/");
                    long restaurantId = Long.parseLong(parts[2]);
                    long itemId = Long.parseLong(parts[4]);
                    new DeleteFoodItemHandler(restaurantId, itemId).handle(exchange);
                    return;
                } catch (NumberFormatException e) {
                    sendJson(exchange, 400, "{\"error\":\"Invalid restaurant or item ID\"}");
                    return;
                }
            }

            if (method.equalsIgnoreCase("POST") && path.matches("^/restaurants/\\d+/menu$")) {
                try {
                    long restaurantId = Long.parseLong(path.split("/")[2]);
                    new AddMenuHandler(restaurantId).handle(exchange);
                    return;
                } catch (NumberFormatException e) {
                    sendJson(exchange, 400, "{\"error\":\"Invalid restaurant ID\"}");
                    return;
                }
            }

            //delete item from menu
            if (method.equals("DELETE") && path.matches("^/restaurants/\\d+/menu/[^/]+/\\d+$")) {
                String[] parts = path.split("/");
                long restaurantId = Long.parseLong(parts[2]);
                String menuTitle = parts[4];
                long itemId = Long.parseLong(parts[5]);
                new DeleteItemFromMenuHandler(restaurantId, menuTitle, itemId).handle(exchange);
                return;
            }

            if (method.equalsIgnoreCase("DELETE") && path.matches("^/restaurants/\\d+/menu/.+$")) {
                String[] parts = path.split("/");
                long restaurantId = Long.parseLong(parts[2]);
                String title = parts[4];
                new DeleteMenuHandler(restaurantId, title).handle(exchange);
                return;
            }

            if (method.equals("PUT") && path.matches("^/restaurants/\\d+/menu/[^/]+$")) {
                String[] parts = path.split("/");
                long restaurantId = Long.parseLong(parts[2]);
                String menuTitle = parts[4];
                new AddItemToMenuHandler(restaurantId, menuTitle).handle(exchange);
                return;
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

    static class VendorRouter implements HttpHandler{
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            if (path.equals("/vendors") && method.equalsIgnoreCase("POST")) {
                new ListVendorsHandler().handle(exchange);
                return;
            }

            if ("GET".equalsIgnoreCase(method) && path.matches("^/vendors/\\d+")) {
                new GetVendorMenuHandler().handle(exchange);
                return;
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
