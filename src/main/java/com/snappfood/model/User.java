package com.snappfood.model;


import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Data;

import javax.persistence.*;
import java.util.HashSet;

enum Role{
    buyer,
    seller,
    courier
    //FIXME : ADMIN?
}

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;

    @Column(nullable = false)
    private String fullname;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String profilePicture;

    @Column(nullable = false)
    private String bankName;

    @Column(nullable = false)
    private String accountNumber;

    //TODO : CART

    //TODO : ORDER HISTORY

    @ManyToMany
    @JoinTable(
            name = "favorite restaurants",
            joinColumns = @JoinColumn(name = "userId"),
            inverseJoinColumns = @JoinColumn(name = "restaurantId")
    )
    private HashSet<Restaurant> favoriteRestaurants;

    @Embedded
    private DeliveryOrders delivery;//for non couriers it'll always be null
}
