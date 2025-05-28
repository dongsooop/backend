package com.dongsoop.dongsoop.meal.repository;

import com.dongsoop.dongsoop.meal.entity.MealDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface MealDetailsRepository extends JpaRepository<MealDetails, Long> {

    // 배치 삭제 최적화
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM MealDetails m WHERE m.mealDate < :cutoffDate")
    int deleteOldMealData(@Param("cutoffDate") LocalDate cutoffDate);
}
