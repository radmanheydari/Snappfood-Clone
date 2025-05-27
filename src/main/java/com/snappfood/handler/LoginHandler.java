package com.snappfood.handler;

import com.google.gson.Gson;
import com.snappfood.model.User;
import com.snappfood.repository.UserRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

public class LoginHandler implements HttpHandler {
    private final Gson gson = new Gson();
    private final UserRepository userRepository = new UserRepository();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("POST".equals(exchange.getRequestMethod())) {
            InputStream is = exchange.getRequestBody();
            String requestBody = new String(is.readAllBytes());
            Map<String, String> userData = gson.fromJson(requestBody, Map.class);

            String phone = userData.get("phone");
            String password = userData.get("password");

            if (phone == null || password == null) {
                sendResponse(exchange, 400, "invalid input");
                return;
            }


            sendResponse(exchange, 200, "User registered successfully");
        } else {
            sendResponse(exchange, 405, "Method not allowed");
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        exchange.sendResponseHeaders(statusCode, message.length());
        OutputStream os = exchange.getResponseBody();
        os.write(message.getBytes());
        os.close();
    }
}