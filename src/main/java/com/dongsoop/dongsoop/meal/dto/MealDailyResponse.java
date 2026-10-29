package com.dongsoop.dongsoop.meal.dto;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
