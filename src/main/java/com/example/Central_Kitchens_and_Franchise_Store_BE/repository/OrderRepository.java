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

    List<Order> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    // ✅ Dùng trong SupplyService.aggregateDailyOrders() và previewAggregation()
    //    Query theo ngày TẠO ĐƠN (createdAt), không phải ngày nhận hàng (orderDate)
    List<Order> findByStatusOrderAndCreatedAtBetween(
            OrderStatus statusOrder,
            LocalDateTime startOfDay,
            LocalDateTime endOfDay);

    // ✅ Dùng trong SupplyService.flushHighPriorityOrders()
    List<Order> findByStatusOrderAndCreatedAtBetweenAndPriorityLevel(
            OrderStatus statusOrder,
            LocalDateTime startOfDay,
            LocalDateTime endOfDay,
            Integer priorityLevel);

    List<Order> findByStatusOrderAndOrderDateAndPriorityLevel(OrderStatus orderStatus, LocalDate date, int i);

    List<Order> findByStatusOrderAndOrderDate(OrderStatus orderStatus, LocalDate date);

    @Query("""
        SELECT o FROM Order o
        WHERE MONTH(o.orderDate) = :month
        AND YEAR(o.orderDate) = :year
        ORDER BY o.orderDate DESC, o.createdAt DESC
    """)
    List<Order> findAllByMonth(
            @Param("month") int month,
            @Param("year")  int year
    );

    // ✅ Get orders by month + year + store
    @Query("""
        SELECT o FROM Order o
        WHERE MONTH(o.orderDate) = :month
        AND YEAR(o.orderDate) = :year
        AND o.storeId = :storeId
        ORDER BY o.orderDate DESC, o.createdAt DESC
    """)
    List<Order> findAllByMonthAndStore(
            @Param("month")   int month,
            @Param("year")    int year,
            @Param("storeId") String storeId
    );

    // ✅ Get orders by store only (all time)
    List<Order> findByStoreIdOrderByOrderDateDescCreatedAtDesc(String storeId);

    // ✅ Count orders by month for summary
    @Query("""
        SELECT COUNT(o) FROM Order o
        WHERE MONTH(o.orderDate) = :month
        AND YEAR(o.orderDate) = :year
        AND o.storeId = :storeId
    """)
    long countByMonthAndStore(
            @Param("month")   int month,
            @Param("year")    int year,
            @Param("storeId") String storeId
    );

    @Query("SELECT o FROM Order o WHERE o.orderDate = :orderDate")
    List<Order> findByOrderDate(@Param("orderDate") LocalDate orderDate);
}
