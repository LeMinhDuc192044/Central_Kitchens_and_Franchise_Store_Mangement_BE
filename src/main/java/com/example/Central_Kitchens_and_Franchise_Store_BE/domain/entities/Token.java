package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.TokenType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "Token")
public class Token {
    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false, unique = true)
    private String refreshToken;

    @Builder.Default
    private boolean expired = false;

    @Builder.Default
    private boolean revoked = false;

    @CreationTimestamp
    private LocalDateTime expiredAt;

    @Enumerated(EnumType.STRING)
    private TokenType tokenType;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
