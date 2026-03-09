package com.example.Central_Kitchens_and_Franchise_Store_BE.mapper;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.OrderStatus;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.ShipmentStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class GhnStatusMapper {

    public OrderStatus toOrderStatus(String ghnStatus) {
        return switch (ghnStatus.toLowerCase()) {
            case "ready_to_pick"                    -> OrderStatus.READY_TO_PICK;
            case "picking"                          -> OrderStatus.PICKING;
            case "picked"                           -> OrderStatus.PICKED;
            case "delivering"                       -> OrderStatus.DELIVERING;
            case "delivered"                        -> OrderStatus.DELIVERED;
            case "delivery_fail"                    -> OrderStatus.DELIVERY_FAILED;
            case "waiting_to_return"                -> OrderStatus.WAITING_TO_RETURN;
            case "return_transporting", "returned"  -> OrderStatus.RETURNED;
            case "cancel"                           -> OrderStatus.CANCELLED;
            default -> {
                log.warn("Unknown GHN status: {}", ghnStatus);
                yield OrderStatus.PENDING;
            }
        };
    }

    public ShipmentStatus toShipmentStatus(String ghnStatus) {
        if (ghnStatus == null) return ShipmentStatus.READY_TO_PICK;

        return switch (ghnStatus.toLowerCase()) {
            case "ready_to_pick" -> ShipmentStatus.READY_TO_PICK;
            case "picking" -> ShipmentStatus.PICKING;
            case "picked" -> ShipmentStatus.PICKED;
            case "storing", "transporting",
                 "sorting", "delivering" -> ShipmentStatus.DELIVERING;
            case "delivered" -> ShipmentStatus.DELIVERED;
            case "delivery_fail" -> ShipmentStatus.DELIVERY_FAILED;
            case "waiting_to_return", "return_transporting" -> ShipmentStatus.WAITING_TO_RETURN;
            case "returned" -> ShipmentStatus.RETURNED;
            case "cancel" -> ShipmentStatus.CANCELLED;
            default -> {
                log.warn("Unknown GHN status: {}", ghnStatus);
                yield ShipmentStatus.READY_TO_PICK;
            }
        };
    }


}
