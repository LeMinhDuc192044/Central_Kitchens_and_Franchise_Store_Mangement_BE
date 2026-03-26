package com.example.Central_Kitchens_and_Franchise_Store_BE.repository;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.FranchiseStorePaymentRecord;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.PaymentRecordType;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FranchiseStorePaymentRecordRepository
        extends JpaRepository<FranchiseStorePaymentRecord, String> {

    List<FranchiseStorePaymentRecord> findByStoreIdOrderByCreatedAtDesc(String storeId);

    List<FranchiseStorePaymentRecord> findByStoreId(String storeId);

    Optional<FranchiseStorePaymentRecord> findByStoreIdAndStatusAndRecordType(
            String storeId, PaymentStatus status, PaymentRecordType recordType);

    // ── Find all DEBT records for this store ───────────────────────────────
    List<FranchiseStorePaymentRecord> findByStoreIdAndRecordType(
            String storeId, PaymentRecordType recordType);

    // ── Find all unpaid MONTHLY records past pay date ──────────────────────
    @Query("SELECT r FROM FranchiseStorePaymentRecord r " +
            "WHERE r.recordType = com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.PaymentRecordType.MONTHLY " +
            "AND r.status = com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.PaymentStatus.PENDING " +
            "AND r.payDate < :now")
    List<FranchiseStorePaymentRecord> findOverdueMonthlyRecords(
            @Param("now") LocalDateTime now);
}
