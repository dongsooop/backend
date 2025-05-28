package com.dongsoop.dongsoop.meal.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MealWeeklyResponse {
    private LocalDate startDate;
    private LocalDate endDate;
    private List<MealDailyResponse> dailyMeals;

}