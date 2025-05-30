package com.snappfood;

import com.google.gson.Gson;
import com.snappfood.handler.user.GetCurrentUserHandler;
import com.snappfood.handler.user.LoginHandler;
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
        server.createContext("/auth/profile", new GetCurrentUserHandler());
        server.start();
        System.out.println("Server running on port 8080");
    }
}