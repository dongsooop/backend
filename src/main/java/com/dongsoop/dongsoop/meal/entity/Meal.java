package com.dongsoop.dongsoop.meal.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "meal")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Meal {

    @EmbeddedId
    private MealKey id;

    public Meal(MealDetails mealDetails) {
        this.id = new MealKey(mealDetails);
    }

    @Embeddable
    @AllArgsConstructor
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class MealKey implements java.io.Serializable {

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "meal_details_id")
        private MealDetails mealDetails;

        @Override
        public boolean equals(Object o) {
            return o instanceof MealKey that &&
                    mealDetails != null &&
                    mealDetails.getId() != null &&
                    mealDetails.getId().equals(that.mealDetails.getId());
        }

        @Override
        public int hashCode() {
            return mealDetails != null && mealDetails.getId() != null
                    ? mealDetails.getId().hashCode()
                    : 0;
        }
    }
}