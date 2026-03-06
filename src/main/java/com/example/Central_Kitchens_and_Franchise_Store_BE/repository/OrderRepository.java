package com.example.Central_Kitchens_and_Franchise_Store_BE.repository;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.Order;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
    List<Order> findByStoreId(String storeId);
    Order findTopByOrderByOrderIdDesc();
    List<Order> findByStoreIdAndStatusOrder(String storeId, OrderStatus statusOrder);
    List<Order> findByStatusOrder(OrderStatus statusOrder);

    @Query("SELECT o.storeId FROM Order o WHERE o.orderId = :orderId")
    Optional<String> findStoreIdByOrderId(@Param("orderId") String orderId);

    @Query("SELECT od.orderId FROM OrderDetail od WHERE od.orderDetailId = :orderDetailId")
    String findOrderIdByOrderDetailId(@Param("orderDetailId") String orderDetailId);

    List<Order> findByStoreIdAndOrderDateBetween(
            String storeId,
            LocalDate start,
            LocalDate end
    );
}
