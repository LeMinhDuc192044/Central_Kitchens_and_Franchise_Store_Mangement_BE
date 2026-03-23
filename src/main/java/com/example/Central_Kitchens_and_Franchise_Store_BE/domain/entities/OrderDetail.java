package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "order_detail")
public class OrderDetail {

    @Id
    @Column(name = "order_detail_id")
    private String orderDetailId;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "order_id_fk")
    private String orderId;

    @OneToOne
    @JoinColumn(name = "order_id_fk", referencedColumnName = "order_id",
            insertable = false, updatable = false)
    private Order order;

    @OneToMany(mappedBy = "orderDetail", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderDetailItem> orderDetailItems = new ArrayList<>();

    @OneToOne(mappedBy = "orderDetail", cascade = CascadeType.ALL)
    private Shipment shipment;

    @OneToMany(mappedBy = "orderDetail", cascade = CascadeType.ALL)
    @Builder.Default
    private Set<Feedback> feedbacks = new HashSet<>();

    // ✅ XÓA field orderInvoice — giờ OrderInvoice thuộc về Order, không phải OrderDetail

    public void addOrderDetailItem(OrderDetailItem item) {
        orderDetailItems.add(item);
        item.setOrderDetail(this);
        item.setOrderDetailId(this.orderDetailId);
        this.amount = calculateTotalAmount();
    }

    public BigDecimal calculateTotalAmount() {
        return orderDetailItems.stream()
                .map(OrderDetailItem::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}