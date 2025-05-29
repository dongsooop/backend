package com.dongsoop.dongsoop.meal.repository;

import com.dongsoop.dongsoop.meal.dto.MealListDto;
import com.dongsoop.dongsoop.meal.entity.MealDetails;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MealDetailsRepository extends JpaRepository<MealDetails, Long> {

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM MealDetails m WHERE m.mealDate < :cutoffDate")
    int deleteOldMealData(@Param("cutoffDate") LocalDate cutoffDate);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM MealDetails m WHERE m.mealDate BETWEEN :startDate AND :endDate")
    int deleteByMealDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT MAX(md.mealDate) FROM MealDetails md")
    @QueryHints(@QueryHint(name = "org.hibernate.readOnly", value = "true"))
    Optional<LocalDate> findMaxMealDate();

    @Query("SELECT new com.dongsoop.dongsoop.meal.dto.MealListDto(" +
            "md.id, md.mealDate, md.dayOfWeek, md.mealType, md.menuItems) " +
            "FROM MealDetails md " +
            "WHERE md.mealDate BETWEEN :startDate AND :endDate " +
            "ORDER BY md.mealDate, md.mealType")
    @QueryHints({
            @QueryHint(name = "org.hibernate.readOnly", value = "true"),
            @QueryHint(name = "org.hibernate.fetchSize", value = "50")
    })
    List<MealListDto> findMealsByDateRangeList(@Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate);
}