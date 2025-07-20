package com.snappfood.dto;

import com.snappfood.model.Rating;

import java.time.LocalDateTime;

public class RatingDTO {
    private Long id;
    private int rating;
    private String comment;
    private String imageBase64;
    private LocalDateTime date;
    private String username;  // from user
    private String foodName;  // from food

    public RatingDTO(Rating rating) {
        this.id = rating.getId();
        this.rating = rating.getRating();
        this.comment = rating.getComment();
        this.imageBase64 = rating.getImageBase64();
        this.date = rating.getDate();
        this.username = rating.getUser().getFull_name();
        this.foodName = rating.getFood().getName();
    }
}
