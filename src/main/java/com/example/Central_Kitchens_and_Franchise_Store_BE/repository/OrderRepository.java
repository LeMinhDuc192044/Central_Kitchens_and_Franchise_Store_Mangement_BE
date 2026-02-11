package com.example.Central_Kitchens_and_Franchise_Store_BE.repository;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.Order;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, String> {
    List<Order> findByStoreId(String storeId);
    Order findTopByOrderByOrderIdDesc();
    List<Order> findByStoreIdAndStatusOrder(String storeId, OrderStatus statusOrder);
    List<Order> findByStatusOrder(OrderStatus statusOrder);
}
