package com.example.Central_Kitchens_and_Franchise_Store_BE.repository;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.Role;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(UserRole name);
}
