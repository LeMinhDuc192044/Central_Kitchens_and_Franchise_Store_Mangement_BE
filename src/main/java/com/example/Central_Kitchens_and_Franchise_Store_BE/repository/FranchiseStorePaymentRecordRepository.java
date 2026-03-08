package com.example.Central_Kitchens_and_Franchise_Store_BE.repository;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.FranchiseStorePaymentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FranchiseStorePaymentRecordRepository
        extends JpaRepository<FranchiseStorePaymentRecord, String> {

    List<FranchiseStorePaymentRecord> findByStoreIdOrderByCreatedAtDesc(String storeId);

    List<FranchiseStorePaymentRecord> findByStoreId(String storeId);
}
