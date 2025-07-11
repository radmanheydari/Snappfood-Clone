package com.snappfood.dto;

import com.snappfood.model.Food;
import com.snappfood.model.FoodCategory;
import lombok.Data;

import java.util.List;

@Data
public class FoodItemDTO {
    private Long id;
    private String name;
    private String imageBase64;
    private String description;
    private int price;
    private int supply;
    private List<String> keywords;
    private CategoryDTO category;

    @Data
    public static class CategoryDTO {
        private Long id;
        private String name;
        public CategoryDTO(FoodCategory c) {
            this.id = c.getId();
            this.name = c.getName();
        }
    }

    public FoodItemDTO(Food f) {
        this.id = f.getId();
        this.name = f.getName();
        this.imageBase64 = f.getImageBase64();
        this.description = f.getDescription();
        this.price = f.getPrice();
        this.supply = f.getSupply();
        this.keywords = f.getKeywords();
        this.category = f.getCategory() != null ? new CategoryDTO(f.getCategory()) : null;
    }
}
