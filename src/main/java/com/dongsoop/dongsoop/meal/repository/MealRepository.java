package com.dongsoop.dongsoop.meal.repository;

import com.dongsoop.dongsoop.meal.dto.MealListDto;
import com.dongsoop.dongsoop.meal.entity.Meal;
import com.dongsoop.dongsoop.meal.entity.Meal.MealKey;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MealRepository extends JpaRepository<Meal, MealKey> {

    // 서브쿼리 대신 JOIN 사용으로 성능 향상
    @Query("SELECT MAX(md.mealDate) FROM MealDetails md")
    @QueryHints(@QueryHint(name = "org.hibernate.readOnly", value = "true"))
    Optional<LocalDate> findMaxMealDate();

    // 최적화된 페이징 쿼리 (인덱스 활용)
    @Query(value = "SELECT new com.dongsoop.dongsoop.meal.dto.MealListDto(" +
            "md.id, md.mealDate, md.dayOfWeek, md.mealType, md.menuItems) " +
            "FROM MealDetails md " +
            "WHERE md.mealDate BETWEEN :startDate AND :endDate " +
            "ORDER BY md.mealDate, md.mealType",
            countQuery = "SELECT COUNT(md) FROM MealDetails md " +
                    "WHERE md.mealDate BETWEEN :startDate AND :endDate")
    @QueryHints(@QueryHint(name = "org.hibernate.readOnly", value = "true"))
    Page<MealListDto> findMealsByDateRange(@Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate,
                                           Pageable pageable);

    // 배치 페치 크기 적용
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