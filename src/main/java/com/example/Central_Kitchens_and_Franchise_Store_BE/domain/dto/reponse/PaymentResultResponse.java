package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PaymentResultResponse {
    private String txnRef;
    private Long amount;
    private String orderInfo;
    private PaymentStatus status;
    private String responseCode;
    private String responseMessage;
    private String bankCode;
    private String bankTranNo;
    private String cardType;
    private String vnpayTxnNo;
    private LocalDateTime paidAt;
}
