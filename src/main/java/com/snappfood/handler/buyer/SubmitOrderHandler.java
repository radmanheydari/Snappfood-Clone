package com.snappfood.handler.buyer;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.snappfood.Role;
import com.snappfood.dto.OrderDTO;
import com.snappfood.dto.OrderDTO.Item;
import com.snappfood.model.*;
import com.snappfood.repository.*;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

public class SubmitOrderHandler implements HttpHandler {
    private static final String APPLICATION_JSON = "application/json";
    private static final String SECRET = "YOUR_SECRET_KEY";
    private static final String BEARER_PREFIX = "Bearer ";

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new JsonDeserializer<LocalDateTime>() {
                @Override
                public LocalDateTime deserialize(JsonElement json, Type type, JsonDeserializationContext ctx) throws JsonParseException {
                    return LocalDateTime.parse(json.getAsString());
                }
            })
            .registerTypeAdapter(LocalDateTime.class, new JsonSerializer<LocalDateTime>() {
                @Override
                public JsonElement serialize(LocalDateTime dateTime, Type type, JsonSerializationContext ctx) {
                    return new JsonPrimitive(dateTime.toString());
                }
            })
            .create();

    private final JWTVerifier verifier = JWT.require(Algorithm.HMAC256(SECRET)).build();
    private final UserRepository userRepo = new UserRepository();
    private final FoodRepository foodRepo = new FoodRepository();
    private final CouponRepository couponRepo = new CouponRepository();
    private final RestaurantRepository restRepo = new RestaurantRepository();
    private final OrderRepository orderRepo = new OrderRepository();

    static class SubmitRequest {
        public String delivery_address;
        public Long vendor_id;
        public Long coupon_id;
        public List<Item> items;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        String auth = exchange.getRequestHeaders().getFirst("Authorization");
        if (auth == null || !auth.startsWith(BEARER_PREFIX)) {
            sendJson(exchange, 401, Map.of("error", "Missing Authorization header"));
            return;
        }

        User customer;
        try {
            DecodedJWT jwt = verifier.verify(auth.substring(BEARER_PREFIX.length()).trim());
            long userId = Long.parseLong(jwt.getSubject());
            Optional<User> uopt = userRepo.findById(userId);
            if (uopt.isEmpty() || uopt.get().getRole() != Role.BUYER) {
                sendJson(exchange, 403, Map.of("error", "Forbidden"));
                return;
            }
            customer = uopt.get();
        } catch (Exception e) {
            sendJson(exchange, 401, Map.of("error", "Invalid token"));
            return;
        }

        SubmitRequest req;
        try (InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
            Type type = new TypeToken<SubmitRequest>() {
            }.getType();
            req = gson.fromJson(reader, type);
        }
        if (req == null
                || req.delivery_address == null
                || req.vendor_id == null
                || req.items == null
                || req.items.isEmpty()) {
            sendJson(exchange, 400, Map.of("error", "Missing required fields"));
            return;
        }

        var restOpt = restRepo.findById(req.vendor_id);
        if (restOpt.isEmpty()) {
            sendJson(exchange, 404, Map.of("error", "Vendor not found"));
            return;
        }

        int raw = 0;
        List<Food> foods = new ArrayList<>();
        for (Item it : req.items) {
            Optional<Food> fopt = foodRepo.findById(it.getItem_id());
            if (fopt.isEmpty()) {
                sendJson(exchange, 404, Map.of("error", "Item not found: " + it.getItem_id()));
                return;
            }
            Food f = fopt.get();
            raw += f.getPrice() * it.getQuantity();
            foods.add(f);
        }

        int taxFee = restOpt.get().getTax_fee();
        int addFee = restOpt.get().getAdditional_fee();
        int pay = raw + taxFee + addFee;
        Coupon coupon = null;
        if (req.coupon_id != null) {
            coupon = couponRepo.findById(req.coupon_id).orElse(null);
            if (coupon != null && raw >= coupon.getMin_price()) {
                pay -= coupon.getValue();
            }
        }

        Order o = new Order();
        o.setDelivery_address(req.delivery_address);
        o.setCustomer(customer);
        o.setVendor(restOpt.get().getOwner());
        o.setCoupon(coupon);

        var embItems = new ArrayList<Order.OrderItem>();
        for (Item it : req.items) {
            embItems.add(new Order.OrderItem(it.getItem_id(), it.getQuantity()));
        }
        o.setItems(embItems);
        o.setRaw_price(raw);
        o.setTax_fee(taxFee);
        o.setAdditional_fee(addFee);
        o.setPay_price(pay);
        o.setStatus("submitted");

        LocalDateTime now = LocalDateTime.now();
        o.setCreatedAt(now);
        o.setUpdatedAt(now);

        Order saved = orderRepo.save(o);
        sendJson(exchange, 200, new OrderDTO(saved));
    }

    private void sendJson(HttpExchange ex, int status, Object body) throws IOException {
        byte[] bytes = gson.toJson(body).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", APPLICATION_JSON);
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }
}
