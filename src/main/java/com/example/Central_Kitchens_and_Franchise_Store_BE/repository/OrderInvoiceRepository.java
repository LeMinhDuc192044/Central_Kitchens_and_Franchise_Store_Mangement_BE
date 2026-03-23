package com.example.Central_Kitchens_and_Franchise_Store_BE.repository;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.OrderInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// OrderInvoiceRepository.java
@Repository
public interface OrderInvoiceRepository extends JpaRepository<OrderInvoice, String> {
    Optional<OrderInvoice> findByOrderId(String orderId);
}
