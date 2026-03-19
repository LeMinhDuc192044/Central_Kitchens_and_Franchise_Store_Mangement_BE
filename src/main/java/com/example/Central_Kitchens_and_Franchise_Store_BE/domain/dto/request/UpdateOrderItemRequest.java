package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.request;

import jakarta.validation.constraints.Min;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderItemRequest {

    private String centralFoodId;   // null = không đổi food, chỉ đổi quantity

    @Min(value = 0, message = "Quantity must be >= 0 (0 = xóa item)")
    private Integer quantity;       // 0 = xóa item khỏi order
}