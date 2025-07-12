package com.snappfood.dto;

import com.snappfood.model.Food;
import lombok.Data;

import java.util.List;

@Data
public class ItemDTO {
    private Long id;
    private String name;
    private String description;
    private int price;
    private int supply;
    private String imageBase64;
    private List<String> keywords;
    private Long category_id;

    public ItemDTO(Food f) {
        this.id = f.getId();
        this.name = f.getName();
        this.description = f.getDescription();
        this.price = f.getPrice();
        this.supply = f.getSupply();
        this.imageBase64 = f.getImageBase64();
        this.keywords = f.getKeywords();
        this.category_id = f.getCategory() != null ? f.getCategory().getId() : null;
    }
}
