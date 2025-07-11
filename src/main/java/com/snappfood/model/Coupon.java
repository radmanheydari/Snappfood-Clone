package com.snappfood.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "coupons")
@Data
@NoArgsConstructor
public class Coupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String coupon_code;

    @Column(nullable = false, unique = true)
    private String type;

    @Column(name = "value", nullable = false)
    private int value;

    @Column(name = "min_price", nullable = false)
    private int min_price;

    @Column(name = "user_count", nullable = false)
    private int user_count;

    @Column(name = "valid_from", nullable = false)
    private OffsetDateTime start_date;

    @Column(name = "valid_until", nullable = false)
    private OffsetDateTime end_date;
}
