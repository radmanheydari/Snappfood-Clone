package com.snappfood.handler.admin;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.snappfood.model.Coupon;
import com.snappfood.model.User;
import com.snappfood.repository.CouponRepository;
import com.snappfood.repository.UserRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

public class CreateCouponHandler implements HttpHandler {
    private static final String APPLICATION_JSON = "application/json";
    private static final String SECRET           = "YOUR_SECRET_KEY";
    private static final String BEARER_PREFIX    = "Bearer ";

    private final Gson            gson       = new Gson();
    private final JWTVerifier     verifier   = JWT.require(Algorithm.HMAC256(SECRET)).build();
    private final UserRepository  userRepo   = new UserRepository();
    private final CouponRepository couponRepo = new CouponRepository();

    @Override
    public void handle(HttpExchange exchange) {
        try {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                send(exchange, 405, Map.of("error","Method not allowed"));
                return;
            }

            // auth
            String auth = exchange.getRequestHeaders().getFirst("Authorization");
            if (auth == null || !auth.startsWith(BEARER_PREFIX)) {
                send(exchange, 401, Map.of("error","Missing or invalid Authorization header"));
                return;
            }
            var jwt = verifier.verify(auth.substring(BEARER_PREFIX.length()).trim());
            long userId = Long.parseLong(jwt.getSubject());
            Optional<User> uopt = userRepo.findById(userId);
            if (uopt.isEmpty() || !"admin".equals(uopt.get().getPhone())) {
                send(exchange, 403, Map.of("error","Forbidden"));
                return;
            }

            // parse body
            Type mapType = new TypeToken<Map<String,Object>>(){}.getType();
            Map<String,Object> data = gson.fromJson(
                    new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8),
                    mapType
            );

            // validate
            String code = (String)data.get("coupon_code");
            String type = (String)data.get("type");
            Number value    = (Number)data.get("value");
            Number minPrice = (Number)data.get("min_price");
            Number userCount= (Number)data.get("user_count");
            String start    = (String)data.get("valid_from");
            String end      = (String)data.get("valid_until");

            if (code==null||type==null||value==null||minPrice==null||userCount==null||start==null||end==null) {
                send(exchange, 400, Map.of("error","Missing required fields"));
                return;
            }

            // build entity
            Coupon c = new Coupon();
            c.setCoupon_code(code);
            c.setType(type);
            c.setValue(value.intValue());
            c.setMin_price(minPrice.intValue());
            c.setUser_count(userCount.intValue());
            c.setStart_date(OffsetDateTime.parse(start));
            c.setEnd_date(OffsetDateTime.parse(end));

            // save
            Coupon saved = couponRepo.save(c);

            send(exchange, 201, saved);

        } catch (com.auth0.jwt.exceptions.JWTVerificationException e) {
            send(exchange, 401, Map.of("error","Invalid or expired token"));
        } catch (Exception e) {
            e.printStackTrace();
            send(exchange, 500, Map.of("error","Internal server error"));
        }
    }

    private void send(HttpExchange ex, int status, Object body) {
        try {
            byte[] bytes = gson.toJson(body).getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().set("Content-Type", APPLICATION_JSON+"; charset=UTF-8");
            ex.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(bytes);
            }
        } catch (Exception ignored) {}
    }
}
