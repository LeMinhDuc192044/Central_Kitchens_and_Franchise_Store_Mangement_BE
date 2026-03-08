package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse;

public record PaymentMethodResponse(
        String storePaymentId,
        String storeId,
        String paymentMethod
) {}
