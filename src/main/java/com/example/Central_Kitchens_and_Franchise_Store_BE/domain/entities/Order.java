package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.OrderStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "orders")
public class Order {

    @Id
    @Column(name = "order_id")
    private String orderId;

    @Column(name = "priority_level")
    @Min(value = 1, message = "Priority level must be between 1 and 3")
    @Max(value = 3, message = "Priority level must be between 1 and 3")
    private Integer priorityLevel;

    @Column(name = "note")
    private String note;

    @Column(name = "order_date")
    @CreationTimestamp
    private LocalDate orderDate;

    @Column(name = "status_order")
    @Enumerated(EnumType.STRING)
    private OrderStatus statusOrder;

    @Column(name = "store_id_fk")
    private String storeId;

    @ManyToOne
    @JoinColumn(name = "store_id_fk", referencedColumnName = "store_id", insertable = false, updatable = false)
    private FranchiseStore franchiseStore;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderDetail> orderDetails = new ArrayList<>();

    // Helper method để thêm OrderDetail
    public void addOrderDetail(OrderDetail orderDetail) {
        orderDetails.add(orderDetail);
        orderDetail.setOrder(this);
        orderDetail.setOrderId(this.orderId);
        orderDetail.setStoreId(this.storeId);
    }

    // Helper method để remove OrderDetail
    public void removeOrderDetail(OrderDetail orderDetail) {
        orderDetails.remove(orderDetail);
        orderDetail.setOrder(null);
    }
}