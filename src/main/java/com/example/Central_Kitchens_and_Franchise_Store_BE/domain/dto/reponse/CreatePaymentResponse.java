package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CreatePaymentResponse {
    private String txnRef;       // Mã giao dịch
    private String paymentUrl;   // URL redirect sang VNPay
    private Long amount;
    private List<String> orderIds;
}
