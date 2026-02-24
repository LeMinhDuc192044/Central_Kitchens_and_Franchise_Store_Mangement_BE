package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.FoodItem;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "order_detail_item")
public class OrderDetailItem {

    @Id
    @Column(name = "order_detail_item_id")
    private String orderDetailItemId;

    @Column(name = "food_item")
    @Enumerated(EnumType.STRING)
    private FoodItem foodItem;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "unit_price")
    private BigDecimal unitPrice;

    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    @Column(name = "order_detail_id_fk")
    private String orderDetailId;

    @ManyToOne
    @JoinColumn(name = "order_detail_id_fk", referencedColumnName = "order_detail_id", insertable = false, updatable = false)
    private OrderDetail orderDetail;
}