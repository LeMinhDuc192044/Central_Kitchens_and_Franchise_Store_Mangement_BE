package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse;

import lombok.Data;

@Data
public class OrderInfoData {
    private String order_code;
    private String status;
    private String to_name;
    private String to_address;
    private String to_phone;
    private Integer cod_amount;
    private Integer weight;
    private String updated_date;
    private String created_date;
    // add more fields as needed
}
