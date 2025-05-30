package com.dongsoop.dongsoop.meal.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Entity
@Table(name = "meal")
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Meal {

    @EmbeddedId
    private MealKey id;

    public Meal(MealDetails mealDetails) {
        this.id = new MealKey(mealDetails);
    }

    @Embeddable
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    public static class MealKey implements java.io.Serializable {

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "meal_details_id")
        private MealDetails mealDetails;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            MealKey that = (MealKey) o;
            return Objects.equals(mealDetails, that.mealDetails);
        }

        @Override
        public int hashCode() {
            return Objects.hash(mealDetails);
        }
    }
}