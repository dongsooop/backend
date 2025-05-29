package com.dongsoop.dongsoop.meal.controller;

import com.dongsoop.dongsoop.meal.dto.MealWeeklyResponse;
import com.dongsoop.dongsoop.meal.service.MealService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/meal")
@RequiredArgsConstructor
@Slf4j
public class MealController {

    private static final CacheControl CACHE_CONTROL = CacheControl
            .maxAge(30, TimeUnit.MINUTES)
            .cachePublic();

    private final MealService mealService;

    @GetMapping("/current")
    public ResponseEntity<MealWeeklyResponse> getCurrentWeekMeal() {
        log.info("현재 주 식단 조회 요청");

        MealWeeklyResponse result = mealService.getCurrentWeekMealResponse();

        log.info("현재 주 식단 조회 결과 - {}일간 데이터", result.getDailyMeals().size());

        return ResponseEntity.ok()
                .cacheControl(CACHE_CONTROL)
                .body(result);
    }
}