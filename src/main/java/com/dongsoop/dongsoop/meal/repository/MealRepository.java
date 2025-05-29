package com.dongsoop.dongsoop.meal.repository;

import com.dongsoop.dongsoop.meal.entity.Meal;
import com.dongsoop.dongsoop.meal.entity.Meal.MealKey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MealRepository extends JpaRepository<Meal, MealKey> {
}