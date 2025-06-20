package com.snappfood;

import com.google.gson.Gson;
import com.snappfood.handler.restaurant.CreateNewRestaurantHandler;
import com.snappfood.handler.restaurant.GetListOfSellersRestaurantHandler;
import com.snappfood.handler.restaurant.UpdateRestaurantHandler;
import com.snappfood.handler.user.CurrentUserHandler;
import com.snappfood.handler.user.LoginHandler;
import com.snappfood.handler.user.LogoutHandler;
import com.snappfood.handler.user.RegisterHandler;
import com.snappfood.repository.UserRepository;
import com.snappfood.service.UserService;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class Main {
    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        UserRepository userRepository = new UserRepository();
        UserService userService = new UserService(userRepository);
        Gson gson = new Gson();
        server.createContext("/auth/register", new RegisterHandler());
        server.createContext("/auth/login", new LoginHandler());
        server.createContext("/auth/profile", new CurrentUserHandler());
        server.createContext("/auth/logout", new LogoutHandler());
        server.createContext("/restaurants", new CreateNewRestaurantHandler());
        server.createContext("/restaurants/mine", new GetListOfSellersRestaurantHandler());

        server.createContext("/restaurant/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            String[] parts = path.split("/");

            if (parts.length == 3 && parts[2].matches("\\d+")) {
                long restaurantId = Long.parseLong(parts[2]);

                switch (exchange.getRequestMethod()) {
                    case "PUT":
                        new UpdateRestaurantHandler(restaurantId).handle(exchange);
                        break;
                    // Can add other methods later (GET, DELETE, etc.)
                    default:
                        exchange.sendResponseHeaders(405, -1); // Method Not Allowed
                }
            } else {
                exchange.sendResponseHeaders(404, -1); // Not Found
            }
        });
        server.start();
        System.out.println("Server running on port 8080");
    }
}