package com.dongsoop.dongsoop.meal.repository;

import com.dongsoop.dongsoop.meal.entity.Meal;
import com.dongsoop.dongsoop.meal.entity.Meal.MealKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface MealRepository extends JpaRepository<Meal, MealKey> {

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Meal m WHERE m.id.mealDetails.mealDate BETWEEN :startDate AND :endDate")
    int deleteByMealDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}