package com.example.Central_Kitchens_and_Franchise_Store_BE.repository;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.Shipment;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.ShipmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, String> {
    Optional<Shipment> findByGhnOrderCode(String ghnOrderCode);


    @Query("SELECT s FROM Shipment s " +
            "JOIN OrderDetail od ON s.orderDetailId = od.orderDetailId " +
            "JOIN Order o ON od.orderId = o.orderId " +
            "WHERE o.storeId = :storeId " +
            "ORDER BY s.createdAt DESC")
    List<Shipment> findAllByStoreId(@Param("storeId") String storeId);

    @Query("SELECT s FROM Shipment s " +
            "JOIN OrderDetail od ON s.orderDetailId = od.orderDetailId " +
            "JOIN Order o ON od.orderId = o.orderId " +
            "WHERE o.storeId = :storeId " +
            "AND s.shipStatus = :status " +
            "ORDER BY s.createdAt DESC")
    List<Shipment> findAllByStoreIdAndStatus(
            @Param("storeId") String storeId,
            @Param("status") ShipmentStatus status);
}
