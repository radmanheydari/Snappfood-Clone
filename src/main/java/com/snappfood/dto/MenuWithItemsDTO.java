package com.snappfood.dto;

import com.snappfood.model.Menu;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

@Data
public class MenuWithItemsDTO {
    private Long id;
    private String title;
    private List<FoodItemDTO> items;

    public MenuWithItemsDTO(Menu m) {
        this.id = m.getId();
        this.title = m.getTitle();
        this.items = m.getFoodItems().stream()
                .map(FoodItemDTO::new)
                .collect(Collectors.toList());
    }
}
