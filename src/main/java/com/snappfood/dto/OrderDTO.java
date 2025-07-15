package com.snappfood.dto;

import com.snappfood.model.Order;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {
    private Long id;
    private String delivery_address;
    private Long customer_id;
    private Long vendor_id;
    private Long coupon_id;
    private List<Item> items;
    private Integer raw_price;
    private Integer tax_fee;
    private Integer additional_fee;
    private Integer courier_fee;
    private Integer pay_price;
    private Long courier_id;
    private String status;
    private LocalDateTime created_at;
    private LocalDateTime updated_at;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private Long item_id;
        private Integer quantity;
    }

    public OrderDTO(Order o) {
        this.id                = o.getId();
        this.delivery_address  = o.getDelivery_address();
        this.customer_id       = o.getCustomer().getId();
        this.vendor_id         = o.getVendor().getId();
        this.coupon_id         = o.getCoupon() != null ? o.getCoupon().getId() : null;
        this.items             = o.getItems().stream()
                .map(i -> new Item(i.getItemId(), i.getQuantity()))
                .collect(Collectors.toList());
        this.raw_price         = o.getRaw_price();
        this.tax_fee           = o.getTax_fee();
        this.additional_fee    = o.getAdditional_fee();
        this.courier_fee       = o.getCourier_fee();
        this.pay_price         = o.getPay_price();
        this.courier_id        = o.getCourier() != null ? o.getCourier().getId() : null;
        this.status            = o.getStatus();
        this.created_at        = o.getCreatedAt();
        this.updated_at        = o.getUpdatedAt();
    }
}
