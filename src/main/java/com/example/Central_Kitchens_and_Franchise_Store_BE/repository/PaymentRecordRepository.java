package com.example.Central_Kitchens_and_Franchise_Store_BE.repository;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.FranchiseStorePaymentRecord;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.PaymentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository

public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, Long> {
    boolean existsByOrderId(String orderId);
    Optional<PaymentRecord> findByOrderId(String orderId);      // ← đổi thành Optional
    Optional<PaymentRecord> findByTxnRef(String txnRef);        // ← đổi thành Optional
    List<PaymentRecord> findAllByTxnRef(String txnRef);

}