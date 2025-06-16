package com.snappfood.dto;

import com.snappfood.model.Restaurant;
import lombok.Getter;

@Getter
public class RestaurantDTO {
    private final Long id;
    private final String name;
    private final String address;
    private final String phone;
    private final String logoBase64;
    private final Integer tax_fee;
    private final Integer additional_fee;

    public RestaurantDTO(Restaurant restaurant) {
        this.id = restaurant.getId();
        this.name = restaurant.getName();
        this.address = restaurant.getAddress();
        this.phone = restaurant.getPhone();
        this.logoBase64 = restaurant.getLogoBase64();
        this.tax_fee = restaurant.getTax_fee();
        this.additional_fee = restaurant.getAdditional_fee();
    }
}
