package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums;

public enum OrderStatus {
    PENDING,
    IN_PROGRESS,
    CANCELLED,
    COMPLETED,
    COOKING_DONE,
    WAITING_FOR_UPDATE,
    WAITING_FOR_PRODUCTION,
    READY_TO_PICK,      // GHN: ready_to_pick
    PICKING,            // GHN: picking
    PICKED,             // GHN: picked
    DELIVERING,         // GHN: delivering
    DELIVERED,          // GHN: delivered
    DELIVERY_FAILED,    // GHN: delivery_fail
    WAITING_TO_RETURN,  // GHN: waiting_to_return
    RETURNED,           // GHN: return_transporting / returned
}
