package com.dongsoop.dongsoop.meal.service;

import com.dongsoop.dongsoop.meal.dto.MealListDto;
import com.dongsoop.dongsoop.meal.dto.MealWeeklyResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface MealService {

    Page<MealListDto> getWeeklyMeal(LocalDate startDate, LocalDate endDate, Pageable pageable);

    Page<MealListDto> getCurrentWeekMeal(Pageable pageable);

    MealWeeklyResponse getWeeklyMealResponse(LocalDate startDate, LocalDate endDate);

    MealWeeklyResponse getCurrentWeekMealResponse();

    void manualCrawl();

    void manualCleanup();
}