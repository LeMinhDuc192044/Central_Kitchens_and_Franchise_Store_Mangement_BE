package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.request;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.PaymentMethod;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.PaymentOption;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {

    @NotBlank(message = "Store ID is required")
    private String storeId;

    @NotNull(message = "Payment option is required")
    private PaymentOption paymentOption;

    private String note;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @FutureOrPresent(message = "Ngày nhận hàng không được ở quá khứ")
    private LocalDate orderDate;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod; // CASH hoặc CREDIT

    @NotNull(message = "Order detail is required")
    @Valid
    private OrderDetailRequest orderDetail;
}