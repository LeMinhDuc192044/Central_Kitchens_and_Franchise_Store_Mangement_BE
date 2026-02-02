package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "feedback")
public class Feedback {

    @Id
    @Column(name = "feedback_id")
    private String feedbackId;

    @Column(name = "date")
    private LocalDate date;

    @Column(name = "quality_order")
    private Integer qualityOrder;

    @Column(name = "status_report")
    private String statusReport;

    @Column(name = "delivery_quality_service")
    private Integer deliveryQualityService;

    @Column(name = "problem_description")
    private String problemDescription;

    @Column(name = "evidence_under_food_picture")
    private String evidenceUnderFoodPicture;

    @Column(name = "order_detail_id_fk")
    private String orderDetailId;

    @ManyToOne
    @JoinColumn(name = "order_detail_id_fk", referencedColumnName = "order_detail_id", insertable = false, updatable = false)
    private OrderDetail orderDetail;
}