package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.request;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.PaymentMethod;

public record UpdatePaymentMethodRequest(
        PaymentMethod paymentMethod   // only CASH or CREDIT
) {}
