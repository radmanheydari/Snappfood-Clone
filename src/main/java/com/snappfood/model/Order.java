// com/snappfood/model/Order.java
package com.snappfood.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
public class Order {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "delivery_address", nullable = false)
    private String deliveryAddress;

    @ManyToOne @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne @JoinColumn(name = "vendor_id", nullable = false)
    private User vendor;

    @ManyToOne @JoinColumn(name = "coupon_id")
    private Coupon coupon;

    @ElementCollection
    @CollectionTable(
            name = "order_items",
            joinColumns = @JoinColumn(name = "order_id")
    )
    @Column(name = "food_id", nullable = false)
    private List<Long> itemIds;

    @Column(name = "raw_price", nullable = false)
    private int rawPrice;

    @Column(name = "tax_fee", nullable = false)
    private int taxFee;

    @Column(name = "additional_fee", nullable = false)
    private int additionalFee;

    @Column(name = "courier_fee", nullable = false)
    private int courierFee;

    @Column(name = "pay_price", nullable = false)
    private int payPrice;

    @ManyToOne @JoinColumn(name = "courier_id")
    private User courier;

    @Column(nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
