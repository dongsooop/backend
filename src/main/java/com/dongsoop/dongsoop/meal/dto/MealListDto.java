package com.dongsoop.dongsoop.meal.dto;

import com.dongsoop.dongsoop.meal.entity.MealType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MealListDto {
    private Long id;
    private LocalDate mealDate;
    private String dayOfWeek;
    private MealType mealType;
    private String menuItems;
}