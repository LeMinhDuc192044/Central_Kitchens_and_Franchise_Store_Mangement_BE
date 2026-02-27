package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.request;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.RequiredNote;
import com.example.Central_Kitchens_and_Franchise_Store_BE.integration.ghn.GhnItem;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateDeliveryOrderRequest {

    @NotNull(message = "Payment type should not be null!! 1 for sender pays and 2 for receiver pay")
    @Min(1)
    @Max(2)
    private Integer payment_type_id;   // 1=sender pays, 2=receiver pays

    private String note;

    @NotNull(message = "Required note must not be blank and should be 'CHOTHUHANG', 'CHOXEMHANGKHONGTHU', 'KHONGCHOXEMHANG'")
    private RequiredNote required_note;     // CHOTHUHANG, CHOXEMHANGKHONGTHU, KHONGCHOXEMHANG

    @NotBlank(message = "Name of the receiver must not be blank!!!")
    private String to_name;

    @NotBlank(message = "Phone of the receiver must not be blank!!!")
    private String to_phone;

    @NotBlank(message = "Address of the receiver must not be blank!!!")
    private String to_address;

    private String to_ward_code;
    private Integer to_district_id;

    private Integer cod_amount;       // Cash on delivery amount

    @NotNull(message = "Weight must not be blank!!!!")
    private Integer weight;           // grams

    @NotNull(message = "Length must not be blank!!!!")
    private Integer length;           // cm

    @NotNull(message = "Width must not be blank!!!!")
    private Integer width;            // cm

    @NotNull(message = "Height must not be blank!!!!")
    private Integer height;           // cm

    @NotNull
    @Min(1)
    @Max(2)
    private Integer service_type_id;  // 1=Express, 2=Standard

    @NotBlank(message = "Need orderDetail Id to know who order is this?")
    private String orderDetailId;

    @NotEmpty(message = "Food list must not be empty")
    private Map<String, Integer> foods;
}
