package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GhnResponse<T> {

    private Integer code;
    private String message;
    private T data;
}
