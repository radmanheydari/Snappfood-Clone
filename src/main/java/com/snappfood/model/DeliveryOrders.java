package com.snappfood.model;

import lombok.Data;
import javax.persistence.*;

@Embeddable
@Data
public class DeliveryOrders {
    @Column
    private int orderId;

    @Column(name = "delivery_status")
    private String status;

    @Column(name = "delivery_date")
    private String deliveryDate;

    @ManyToOne
    @JoinColumn(name = "courier_id")
    private User courier;
}