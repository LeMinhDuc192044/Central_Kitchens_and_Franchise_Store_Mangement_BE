package com.example.Central_Kitchens_and_Franchise_Store_BE.integration.ghn;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.OrderStatus;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.ShipmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentStatusUpdateResponse {
    private String shipmentId;
    private String ghnOrderCode;
    private String ghnRawStatus;
    private ShipmentStatus shipmentStatus;
    private String orderDetailId;
    private String orderId;
    private OrderStatus orderStatus;
    private LocalDateTime updatedAt;
}
