package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.request;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.RequiredNote;
import com.example.Central_Kitchens_and_Franchise_Store_BE.integration.ghn.GhnItem;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateDeliveryOrderRequest {

    @NotNull(message = "Payment type should not be null!! 1 for sender pays and 2 for receiver pay")
    @Min(1)
    @Max(2)
    private Integer payment_type_id;   // 1=sender pays, 2=receiver pays

    private String note;

    @NotBlank(message = "Name of the receiver must not be blank!!!")
    private String to_name;

    @NotBlank(message = "Phone of the receiver must not be blank!!!")
    private String to_phone;

    @NotBlank(message = "Store id must not be blank!!!")
    private String storeId;

    @NotBlank(message = "Need orderDetail Id to know who order is this?")
    private String orderDetailId;


}
