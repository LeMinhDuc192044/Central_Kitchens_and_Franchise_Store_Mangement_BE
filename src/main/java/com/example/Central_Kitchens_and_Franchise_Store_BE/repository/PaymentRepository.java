package com.example.Central_Kitchens_and_Franchise_Store_BE.repository;


import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByTxnRef(String txnRef);
}
