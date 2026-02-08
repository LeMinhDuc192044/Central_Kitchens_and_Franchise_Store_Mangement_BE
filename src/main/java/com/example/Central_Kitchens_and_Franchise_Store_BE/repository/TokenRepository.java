package com.example.Central_Kitchens_and_Franchise_Store_BE.repository;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TokenRepository extends JpaRepository<Token, Long> {
    @Query("""
        SELECT t FROM Token t 
        WHERE t.user.id = :userId 
        AND (t.expired = false OR t.revoked = false)
        """)
    List<Token> findAllValidTokensByUser(String userId);
    Optional<Token> findByToken(String token);
    List<Token> findByUserId(String userId);

}
