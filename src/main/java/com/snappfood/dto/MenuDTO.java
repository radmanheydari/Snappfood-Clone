package com.snappfood.dto;

import com.snappfood.model.Menu;
import lombok.Getter;

@Getter
public class MenuDTO {
    private Long id;
    private String title;
    private Long restaurantId;

    public MenuDTO(Menu menu) {
        this.id = menu.getId();
        this.title = menu.getTitle();
        this.restaurantId = menu.getRestaurant() != null ? menu.getRestaurant().getId() : null;
    }
}
