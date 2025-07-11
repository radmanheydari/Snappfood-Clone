package com.snappfood.dto;

import com.snappfood.model.Order;
import lombok.Data;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Data
public class OrderDTO {
    private Long id;
    private String delivery_address;
    private Long customer_id;
    private Long vendor_id;
    private Long coupon_id;
    private List<Long> item_ids;
    private int raw_price;
    private int tax_fee;
    private int additional_fee;
    private int courier_fee;
    private int pay_price;
    private Long courier_id;
    private String status;
    private String created_at;
    private String updated_at;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    public OrderDTO(Order o) {
        this.id = o.getId();
        this.delivery_address = o.getDeliveryAddress();
        this.customer_id = o.getCustomer().getId();
        this.vendor_id = o.getVendor().getId();
        this.coupon_id = o.getCoupon() != null ? o.getCoupon().getId() : null;
        this.item_ids = o.getItemIds();
        this.raw_price = o.getRawPrice();
        this.tax_fee = o.getTaxFee();
        this.additional_fee = o.getAdditionalFee();
        this.courier_fee = o.getCourierFee();
        this.pay_price = o.getPayPrice();
        this.courier_id = o.getCourier() != null ? o.getCourier().getId() : null;
        this.status = o.getStatus();
        this.created_at = o.getCreatedAt().format(FMT);
        this.updated_at = o.getUpdatedAt().format(FMT);
    }
}
