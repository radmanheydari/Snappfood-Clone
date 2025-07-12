package com.snappfood.handler.buyer;

import com.google.gson.Gson;
import com.snappfood.model.Coupon;
import com.snappfood.repository.CouponRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.OutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

public class CheckCouponHandler implements HttpHandler {
    private static final String APPLICATION_JSON = "application/json";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private final Gson gson = new Gson();
    private final CouponRepository repo = new CouponRepository();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        URI uri = exchange.getRequestURI();
        String codeParam = null;
        if (uri.getRawQuery() != null) {
            for (String kv : uri.getRawQuery().split("&")) {
                String[] parts = kv.split("=", 2);
                if (parts.length == 2 && "coupon_code".equals(parts[0])) {
                    codeParam = parts[1];
                    break;
                }
            }
        }

        if (codeParam == null || codeParam.isBlank()) {
            sendJson(exchange, 400, Map.of("error", "Missing required parameter: code"));
            return;
        }

        Optional<Coupon> opt = repo.findByCode(codeParam);
        if (opt.isEmpty()) {
            sendJson(exchange, 404, Map.of(
                    "valid", false,
                    "message", "Coupon not found"
            ));
            return;
        }

        Coupon c = opt.get();
        OffsetDateTime now = OffsetDateTime.now();
        boolean inWindow = !now.isBefore(c.getStart_date()) && !now.isAfter(c.getEnd_date());
        boolean underLimit = c.getUser_count() > 0;
        if (!inWindow) {
            sendJson(exchange, 200, Map.of(
                    "valid", false,
                    "message", "Coupon not yet valid or expired"
            ));
            return;
        }
        if (!underLimit) {
            sendJson(exchange, 200, Map.of(
                    "valid", false,
                    "message", "Coupon usage limit reached"
            ));
            return;
        }

        sendJson(exchange, 200, Map.of(
                "valid", true,
                "coupon_code", c.getCoupon_code(),
                "type", c.getType(),
                "value", c.getValue(),
                "min_price", c.getMin_price(),
                "user_count", c.getUser_count(),
                "start_date", c.getStart_date().format(FMT),
                "end_date", c.getEnd_date().format(FMT)
        ));
    }

    private void sendJson(HttpExchange exchange, int status, Map<String, Object> body) throws IOException {
        byte[] bytes = gson.toJson(body).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", APPLICATION_JSON);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
