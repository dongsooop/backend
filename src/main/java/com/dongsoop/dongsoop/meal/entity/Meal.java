package com.dongsoop.dongsoop.meal.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "meal",
        indexes = {
                @Index(name = "idx_meal_date_type", columnList = "meal_date, meal_type"),
                @Index(name = "idx_meal_date", columnList = "meal_date")
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Meal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "meal_date", nullable = false)
    private LocalDate mealDate;

    @Column(name = "day_of_week", nullable = false, length = 10)
    private String dayOfWeek;

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_type", nullable = false, length = 20)
    private MealType mealType;

    @Column(name = "menu_items", columnDefinition = "TEXT")
    private String menuItems;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Meal meal = (Meal) o;

        if (id == null && meal.id == null) {
            return Objects.equals(mealDate, meal.mealDate) &&
                    Objects.equals(mealType, meal.mealType);
        }

        return Objects.equals(id, meal.id);
    }

    @Override
    public int hashCode() {
        if (id == null) {
            return Objects.hash(mealDate, mealType);
        }

        return Objects.hash(id);
    }
}