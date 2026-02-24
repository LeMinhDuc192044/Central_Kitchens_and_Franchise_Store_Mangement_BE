package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.request;

import lombok.Data;

@Data
public class CreatePaymentRequest {
    private Long amount;        // Số tiền VND (ví dụ: 100000 = 100,000 VNĐ)
    private String orderInfo;   // Thông tin đơn hàng
    private String bankCode;    // Mã ngân hàng (optional)
    private String language;    // "vn" hoặc "en" (optional, mặc định "vn")
}
