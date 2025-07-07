package com.snappfood.model;

import lombok.*;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(
        name = "menus",
        uniqueConstraints = @UniqueConstraint(columnNames = {"restaurant_id", "title"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Menu {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String title;

    @ManyToOne
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @OneToMany(mappedBy = "menu", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Food> foodItems;

}
