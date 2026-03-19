package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums;

public enum PaymentStatus {
    PENDING,
    SUCCESS,
    FAILED,
    CANCELLED,
    REFUNDED,
    PENDING_REFUND // trạng thái trung gian: PENDING → SUCCESS → PENDING_REFUND → REFUNDED
}
