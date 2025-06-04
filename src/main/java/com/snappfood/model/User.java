package com.snappfood.model;


import com.snappfood.Role;
import lombok.*;

import javax.persistence.*;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String full_name;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(nullable = false)
    private String address;

    @Column(nullable = true)
    private String profilePicture;

    @Embedded
    private BankInfo bank_info;

    //TODO : CART

    //TODO : ORDER HISTORY

    @ManyToMany
    @JoinTable(
            name = "user_favorite_restaurants",
            joinColumns = @JoinColumn(name = "userId"),
            inverseJoinColumns = @JoinColumn(name = "restaurantId")
    )
    private Set<Restaurant> favoriteRestaurants;

    @Embedded
    private Order delivery;//for non couriers it'll always be null
}
