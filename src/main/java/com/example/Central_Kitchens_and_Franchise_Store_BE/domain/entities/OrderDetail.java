package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Data
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

    @Column(name = "supply_coordinator_id_fk")
    private String supplyCoordinatorId;

    @Column(name = "store_id_fk")
    private String storeId;

    @Column(name = "order_id_fk")
    private String orderId;

    @ManyToOne
    @JoinColumn(name = "order_id_fk", referencedColumnName = "order_id", insertable = false, updatable = false)
    private Order order;

    @OneToMany(mappedBy = "orderDetail", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderDetailItem> orderDetailItems = new ArrayList<>();

    @OneToOne(mappedBy = "orderDetail", cascade = CascadeType.ALL)
    private Shipment shipment;

    @OneToMany(mappedBy = "orderDetail", cascade = CascadeType.ALL)
    @Builder.Default
    private Set<Feedback> feedbacks = new HashSet<>();

    @OneToOne(mappedBy = "orderDetail", cascade = CascadeType.ALL)
    private OrderInvoice orderInvoice;

    // Helper method để thêm OrderDetailItem
    public void addOrderDetailItem(OrderDetailItem item) {
        orderDetailItems.add(item);
        item.setOrderDetail(this);
        item.setOrderDetailId(this.orderDetailId);
    }

    // Helper method để remove OrderDetailItem
    public void removeOrderDetailItem(OrderDetailItem item) {
        orderDetailItems.remove(item);
        item.setOrderDetail(null);
    }
}