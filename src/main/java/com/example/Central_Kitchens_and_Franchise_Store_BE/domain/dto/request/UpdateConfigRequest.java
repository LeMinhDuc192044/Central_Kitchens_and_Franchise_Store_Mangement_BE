package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

// dto/request/UpdateConfigRequest.java
@Data
public class UpdateConfigRequest {

    @NotNull(message = "Value không được để trống")
    @Min(value = 1, message = "Giá trị phải lớn hơn 0")
    private Integer value;

    private String description; // optional, null = giữ nguyên description cũ
}
