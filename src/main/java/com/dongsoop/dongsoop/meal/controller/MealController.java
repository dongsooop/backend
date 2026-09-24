package com.dongsoop.dongsoop.meal.controller;

import com.dongsoop.dongsoop.meal.dto.MealPriceResponse;
import com.dongsoop.dongsoop.meal.dto.MealWeeklyResponse;
import com.dongsoop.dongsoop.meal.service.MealService;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/meal")
@RequiredArgsConstructor
public class MealController {

    private static final CacheControl CACHE_CONTROL = CacheControl
            .maxAge(30, TimeUnit.MINUTES)
            .cachePublic();

    // 가격은 설정 파일 값이라 배포 전에는 바뀌지 않는다
    private static final CacheControl PRICE_CACHE_CONTROL = CacheControl
            .maxAge(1, TimeUnit.DAYS)
            .cachePublic();

    private final MealService mealService;

    @GetMapping("/current")
    public ResponseEntity<MealWeeklyResponse> getCurrentWeekMeal() {
        MealWeeklyResponse result = mealService.getCurrentWeekMealResponse();

        return ResponseEntity.ok()
                .cacheControl(CACHE_CONTROL)
                .body(result);
    }

    @GetMapping("/prices")
    public ResponseEntity<MealPriceResponse> getPrices() {
        MealPriceResponse result = mealService.getPriceResponse();

        return ResponseEntity.ok()
                .cacheControl(PRICE_CACHE_CONTROL)
                .body(result);
    }
}
