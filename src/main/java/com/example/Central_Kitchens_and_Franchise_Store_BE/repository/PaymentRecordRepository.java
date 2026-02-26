package com.example.Central_Kitchens_and_Franchise_Store_BE.repository;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.PaymentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository

public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, String> {
    boolean existsByOrderId(String orderId);
    List<PaymentRecord> findByOrderId(String orderId);
    PaymentRecord findByTxnRef(String txnRef);
}