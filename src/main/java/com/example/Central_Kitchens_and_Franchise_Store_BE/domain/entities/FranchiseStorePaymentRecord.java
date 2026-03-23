package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.PaymentRecordType;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "franchise_store_payment_record")
    public class FranchiseStorePaymentRecord {

    @Id
    @Column(name = "payment_record_id")
    private String paymentRecordId;

    @Column(name = "debt_amount")
    private BigDecimal debtAmount;

    @Column(name = "store_id_fk")
    private String storeId;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "pay_date")
    @Builder.Default
    private LocalDateTime payDate = LocalDateTime.now().plusMonths(1);

    @Column(name = "status")
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "record_type")
    private PaymentRecordType recordType;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinTable(
            name = "payment_record_orders",
            joinColumns = @JoinColumn(
                    name = "payment_record_id_fk",
                    referencedColumnName = "payment_record_id"),
            inverseJoinColumns = @JoinColumn(
                    name = "order_id_fk",
                    referencedColumnName = "order_id")
    )
    @Builder.Default
    private List<Order> orders = new ArrayList<>();

    public void addOrder(Order order) {
        this.orders.add(order);
    }

    @ManyToOne
    @JoinColumn(name = "store_id_fk", referencedColumnName = "store_id", insertable = false, updatable = false)
    private FranchiseStore franchiseStore;
}
