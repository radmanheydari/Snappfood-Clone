package com.snappfood.integration;

import com.google.gson.*;
import org.junit.jupiter.api.*;

import java.net.http.*;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.URI;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

//FIXME

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EndToEndTest {
    private static final String BASE = "http://localhost:8080";
    private static final Gson gson = new Gson();

    private static HttpClient client;
    private static String sellerToken;
    private static String buyerToken;
    private static long restaurantId;
    private static long foodId;
    private static long orderId;

    @BeforeAll
    static void setup() {
        client = HttpClient.newHttpClient();
    }

    @Test @Order(1)
    void registerSeller() throws Exception {
        String body = gson.toJson(Map.of(
                "full_name","Alice Seller",
                "password","secret1",
                "phone","09110000001",
                "role","SELLER",
                "email","alice@shop.com",
                "address","1 Market St.",
                "profilePicture","",
                "bank_info", Map.of("bank_name","BankA","account_number","123456")
        ));
        var req = HttpRequest.newBuilder()
                .uri(URI.create(BASE+"/auth/register"))
                .header("Content-Type","application/json")
                .POST(BodyPublishers.ofString(body))
                .build();
        var resp = client.send(req, BodyHandlers.ofString());
        assertEquals(200, resp.statusCode());
    }

    @Test @Order(2)
    void registerBuyer() throws Exception {
        String body = gson.toJson(Map.of(
                "full_name","Bob Buyer",
                "password","secret2",
                "phone","09110000002",
                "role","BUYER",
                "email","bob@home.com",
                "address","2 Elm St.",
                "profilePicture","",
                "bank_info", Map.of("bank_name","BankB","account_number","654321")
        ));
        var req = HttpRequest.newBuilder()
                .uri(URI.create(BASE+"/auth/register"))
                .header("Content-Type","application/json")
                .POST(BodyPublishers.ofString(body))
                .build();
        var resp = client.send(req, BodyHandlers.ofString());
        assertEquals(200, resp.statusCode());
    }

    @Test @Order(3)
    void loginSeller() throws Exception {
        String body = gson.toJson(Map.of("email","alice@shop.com","password","secret1"));
        var req = HttpRequest.newBuilder()
                .uri(URI.create(BASE+"/auth/login"))
                .header("Content-Type","application/json")
                .POST(BodyPublishers.ofString(body))
                .build();
        var resp = client.send(req, BodyHandlers.ofString());
        assertEquals(200, resp.statusCode());
        JsonObject jo = JsonParser.parseString(resp.body()).getAsJsonObject();
        sellerToken = jo.get("token").getAsString();
    }

    @Test @Order(4)
    void createRestaurant() throws Exception {
        String body = gson.toJson(Map.of(
                "name","Testaurant",
                "address","123 Food Plaza",
                "phone","09123334455",
                "logoBase64","",
                "tax_fee",500,
                "additional_fee",200
        ));
        var req = HttpRequest.newBuilder()
                .uri(URI.create(BASE+"/restaurants"))
                .header("Content-Type","application/json")
                .header("Authorization","Bearer "+sellerToken)
                .POST(BodyPublishers.ofString(body))
                .build();
        var resp = client.send(req, BodyHandlers.ofString());
        assertEquals(200, resp.statusCode());
        JsonObject jo = JsonParser.parseString(resp.body()).getAsJsonObject();
        restaurantId = jo.getAsJsonObject("restaurant").get("id").getAsLong();
    }

    @Test @Order(5)
    void addFoodItem() throws Exception {
        String body = gson.toJson(Map.of(
                "name","Burger",
                "description","Tasty burger",
                "price",10000,
                "supply",50,
                "keywords", new String[]{"fast","beef"}
        ));
        var req = HttpRequest.newBuilder()
                .uri(URI.create(BASE+"/restaurants/"+restaurantId+"/item"))
                .header("Content-Type","application/json")
                .header("Authorization","Bearer "+sellerToken)
                .POST(BodyPublishers.ofString(body))
                .build();
        var resp = client.send(req, BodyHandlers.ofString());
        assertEquals(200, resp.statusCode());
        JsonObject jo = JsonParser.parseString(resp.body()).getAsJsonObject();
        foodId = jo.get("id").getAsLong();
    }

    @Test @Order(6)
    void loginBuyer() throws Exception {
        String body = gson.toJson(Map.of("email","bob@home.com","password","secret2"));
        var req = HttpRequest.newBuilder()
                .uri(URI.create(BASE+"/auth/login"))
                .header("Content-Type","application/json")
                .POST(BodyPublishers.ofString(body))
                .build();
        var resp = client.send(req, BodyHandlers.ofString());
        assertEquals(200, resp.statusCode());
        JsonObject jo = JsonParser.parseString(resp.body()).getAsJsonObject();
        buyerToken = jo.get("token").getAsString();
    }

    @Test @Order(7)
    void submitOrder() throws Exception {
        String body = gson.toJson(Map.of(
                "delivery_address","45 Baker St.",
                "vendor_id", restaurantId,
                "items", new Object[]{ Map.of("item_id",foodId,"quantity",3) }
        ));
        var req = HttpRequest.newBuilder()
                .uri(URI.create(BASE+"/orders"))
                .header("Content-Type","application/json")
                .header("Authorization","Bearer "+buyerToken)
                .POST(BodyPublishers.ofString(body))
                .build();
        var resp = client.send(req, BodyHandlers.ofString());
        assertEquals(200, resp.statusCode());
        JsonObject jo = JsonParser.parseString(resp.body()).getAsJsonObject();
        orderId = jo.get("id").getAsLong();
    }
}
