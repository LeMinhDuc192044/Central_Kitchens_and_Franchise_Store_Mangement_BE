package com.example.Central_Kitchens_and_Franchise_Store_BE.repository;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.FranchiseStorePaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FranchiseStorePaymentMethodRepository
        extends JpaRepository<FranchiseStorePaymentMethod, String> {

    List<FranchiseStorePaymentMethod> findByStoreId(String storeId);

    Optional<FranchiseStorePaymentMethod> findByStoreIdAndPaymentMethod(
            String storeId, String paymentMethod);
}
