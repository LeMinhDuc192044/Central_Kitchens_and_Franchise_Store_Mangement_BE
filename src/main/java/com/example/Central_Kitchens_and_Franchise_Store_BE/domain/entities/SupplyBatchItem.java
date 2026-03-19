package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "supply_batch_items")
public class SupplyBatchItem {

    @Id
    @Column(name = "item_id")
    private String itemId;

    @Column(name = "central_food_id", nullable = false)
    private String centralFoodId;

    @Column(name = "food_name")
    private String foodName;

    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity;

    // Lưu dạng "STORE-A: 20, STORE-B: 15" — ghi rõ store nào đóng góp bao nhiêu
    @Column(name = "source_detail", columnDefinition = "TEXT")
    private String sourceDetail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private SupplyBatch supplyBatch;
}