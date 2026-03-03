package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.OrderStatus;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.PaymentOption;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;


@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "orders")
public class Order {

    @Id
    @Column(name = "order_id")
    private String orderId;

    @Min(value = 1, message = "Priority level must be between 1 and 3")
    @Max(value = 3, message = "Priority level must be between 1 and 3")
    @Column(name = "priority_level")
    private Integer priorityLevel;

    @Column(name = "note")
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_option")
    private PaymentOption paymentOption;

    @CreationTimestamp
    @Column(name = "order_date")
    private LocalDate orderDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_order")
    private OrderStatus statusOrder;

    @Column(name = "store_id_fk")
    private String storeId;

    @ManyToOne
    @JoinColumn(name = "store_id_fk", referencedColumnName = "store_id",
            insertable = false, updatable = false)
    private FranchiseStore franchiseStore;

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private OrderDetail orderDetail;


    public void assignOrderDetail(OrderDetail orderDetail) {
        this.orderDetail = orderDetail;
        orderDetail.setOrder(this);
        orderDetail.setOrderId(this.orderId);

    }
}