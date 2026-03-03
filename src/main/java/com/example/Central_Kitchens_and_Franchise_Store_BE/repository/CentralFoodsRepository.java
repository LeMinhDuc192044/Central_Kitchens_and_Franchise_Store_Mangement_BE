package com.example.Central_Kitchens_and_Franchise_Store_BE.repository;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.CentralFoods;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

<<<<<<< HEAD
import java.util.List;
=======
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
>>>>>>> 1a07c9a97c3a5bc97aa821c297aa1ba5e676a380

@Repository
public interface CentralFoodsRepository extends JpaRepository<CentralFoods, String> {

<<<<<<< HEAD
    // Lấy danh sách món theo status (dùng cho dropdown FE)
    List<CentralFoods> findByCentralFoodStatus(String status);
}
=======
    Optional<CentralFoods> findByFoodName(String foodName);

    // Find by status
    List<CentralFoods> findByCentralFoodStatus(String status);
    // Find expired foods
    List<CentralFoods> findByExpiryDateBefore(LocalDate date);

    // Find foods expiring soon (within next N days)
    List<CentralFoods> findByExpiryDateBetween(LocalDate startDate, LocalDate endDate);

    List<CentralFoods> findByCentralFoodIdIn(List<String> ids);
}
>>>>>>> 1a07c9a97c3a5bc97aa821c297aa1ba5e676a380
