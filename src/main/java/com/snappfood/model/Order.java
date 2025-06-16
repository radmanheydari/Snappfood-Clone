package com.snappfood.model;

import lombok.Data;
import javax.persistence.*;

@Entity
@Data
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private Long orderId;

    @Column(name = "delivery_address")
    private String delivery_address;

    @Column(name = "vendor_id")
    private int vendor_id;

    @Column(name = "coupon_id")
    private int coupon_id;

    //TODO : ITEMS( ITEM_ID, QUANTITY )

    @ManyToOne
    @JoinColumn(name = "courier_id")
    private User courier;
}