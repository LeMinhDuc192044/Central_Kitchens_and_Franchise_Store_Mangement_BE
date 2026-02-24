package com.example.Central_Kitchens_and_Franchise_Store_BE.util;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.Order;
import com.example.Central_Kitchens_and_Franchise_Store_BE.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderIdGenerator {

    private final OrderRepository orderRepository;
    private static final String PREFIX = "ORD";
    private static final int NUMBER_LENGTH = 3;

    public String generateOrderId() {
        // Lấy order cuối cùng trong DB
        Order lastOrder = orderRepository.findTopByOrderByOrderIdDesc();

        if (lastOrder == null) {
            log.info("Generating first order ID: {}001", PREFIX);
            return PREFIX + "001";
        }

        // Extract số từ orderId cuối
        int currentNumber = extractNumber(lastOrder.getOrderId());
        int nextNumber = currentNumber + 1;

        String newOrderId = String.format("%s%0" + NUMBER_LENGTH + "d", PREFIX, nextNumber);

        log.info("Generated new order ID: {} (previous: {})", newOrderId, lastOrder.getOrderId());

        return newOrderId;
    }

    private int extractNumber(String orderId) {
        try {
            return Integer.parseInt(orderId.substring(PREFIX.length()));
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            log.error("Invalid order ID format: {}", orderId, e);
            throw new IllegalArgumentException("Invalid order ID format: " + orderId);
        }
    }
}
