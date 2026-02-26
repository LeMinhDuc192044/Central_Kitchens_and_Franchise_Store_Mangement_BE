package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse;

import lombok.*;

import java.time.LocalDateTime;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class PaymentResponse {
    private String id;
    private String txnRef;
    private Long amount;
    private String status;
    private String responseCode;
    private String ipAddress;
    private String bankCode;
    private String bankTranNo;
    private String cardType;
    private String vnpayTxnNo;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
    private String orderId;
}
