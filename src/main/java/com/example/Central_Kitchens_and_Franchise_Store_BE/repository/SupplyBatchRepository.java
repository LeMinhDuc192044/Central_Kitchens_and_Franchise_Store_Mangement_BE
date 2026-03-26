package com.example.Central_Kitchens_and_Franchise_Store_BE.repository;



import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.SupplyBatch;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.BatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SupplyBatchRepository extends JpaRepository<SupplyBatch, String> {

    List<SupplyBatch> findByBatchDate(LocalDate batchDate);

    List<SupplyBatch> findByStatus(BatchStatus status);

    List<SupplyBatch> findByBatchDateAndStatus(LocalDate batchDate, BatchStatus status);
    List<SupplyBatch> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    // Trong SupplyBatchRepository
    @Query("SELECT DISTINCT s.batchDate FROM SupplyBatch s ORDER BY s.batchDate ASC")
    List<LocalDate> findAllDistinctBatchDates();
}
