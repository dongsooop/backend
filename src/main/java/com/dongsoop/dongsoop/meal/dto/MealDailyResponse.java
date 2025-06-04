package com.dongsoop.dongsoop.meal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MealDailyResponse {
    private LocalDate date;
    private String dayOfWeek;
    private String koreanMenu;
    private String specialMenu;
}