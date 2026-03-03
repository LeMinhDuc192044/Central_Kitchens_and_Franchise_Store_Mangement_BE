package com.example.Central_Kitchens_and_Franchise_Store_BE.repository;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.CentralFoods;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CentralFoodsRepository extends JpaRepository<CentralFoods, String> {

    // Lấy danh sách món theo status (dùng cho dropdown FE)
    List<CentralFoods> findByCentralFoodStatus(String status);
}