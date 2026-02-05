package com.example.Central_Kitchens_and_Franchise_Store_BE.repository;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.FranchiseStore;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FranchiseStoreRepository extends JpaRepository<FranchiseStore, String> {
}
