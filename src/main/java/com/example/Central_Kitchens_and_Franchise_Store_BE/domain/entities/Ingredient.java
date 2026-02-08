package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "ingredient")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ingredient {

    @Id
    @Column(name = "ingredient_id")
    private String ingredientId;

    @Column(name = "ingredient_name")
    private String ingredientName;

    @Column(name = "quantity")
    private BigDecimal quantity;

    @Column(name = "expire_date")
    private LocalDate expireDate;

    @Column(name = "category_id")
    private String categoryId;

    @Column(name = "manufacturing_date")
    private LocalDate manufacturingDate;

    @Column(name = "status")
    private String status;

    @Column(name = "origin")
    private String origin;

    @Column(name = "import_date")
    private LocalDate importDate;

    @Column(name = "unit_price")
    private BigDecimal unitPrice;

    @ManyToOne
    @JoinColumn(name = "category_id", referencedColumnName = "ingredient_type_id", insertable = false, updatable = false)
    private IngredientCategory ingredientCategory;

    @OneToMany(mappedBy = "ingredient", cascade = CascadeType.ALL)
    @Builder.Default
    private Set<RecipeIngredient> recipeIngredients = new HashSet<>();
}
