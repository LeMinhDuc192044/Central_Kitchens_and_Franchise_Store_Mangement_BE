package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.OrderStatus;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.PaymentMethod;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.PaymentOption;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {

    private String orderId;
    private Integer priorityLevel;
    private LocalDate orderDate;
    private OrderStatus statusOrder;
    private PaymentOption paymentOption;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private String note;
    private OrderDetailResponse orderDetail;
    private String storeId;
    private String cancelReason;

}