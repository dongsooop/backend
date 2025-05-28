package com.dongsoop.dongsoop.meal.controller;

import com.dongsoop.dongsoop.meal.dto.MealListDto;
import com.dongsoop.dongsoop.meal.dto.MealWeeklyResponse;
import com.dongsoop.dongsoop.meal.service.MealService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/meal")
@RequiredArgsConstructor
public class MealController {

    private static final CacheControl CACHE_CONTROL = CacheControl
            .maxAge(30, TimeUnit.MINUTES)
            .cachePublic();
    private static final CacheControl NO_CACHE = CacheControl.noCache();
    private final MealService mealService;

    @GetMapping("/current")
    public ResponseEntity<Page<MealListDto>> getCurrentWeekMeal(
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok()
                .cacheControl(CACHE_CONTROL)
                .body(mealService.getCurrentWeekMeal(pageable));
    }

    @GetMapping("/week")
    public ResponseEntity<Page<MealListDto>> getWeeklyMeal(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok()
                .cacheControl(CACHE_CONTROL)
                .body(mealService.getWeeklyMeal(startDate, endDate, pageable));
    }

    @GetMapping("/current/summary")
    public ResponseEntity<MealWeeklyResponse> getCurrentWeekSummary() {
        return ResponseEntity.ok()
                .cacheControl(CACHE_CONTROL)
                .body(mealService.getCurrentWeekMealResponse());
    }

    @GetMapping("/week/summary")
    public ResponseEntity<MealWeeklyResponse> getWeeklySummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok()
                .cacheControl(CACHE_CONTROL)
                .body(mealService.getWeeklyMealResponse(startDate, endDate));
    }

    @PostMapping("/crawl")
    public ResponseEntity<String> crawlMeal() {
        mealService.manualCrawl();
        return ResponseEntity.ok()
                .cacheControl(NO_CACHE)
                .body("식단 크롤링이 완료되었습니다.");
    }

    @DeleteMapping("/cleanup")
    public ResponseEntity<String> cleanupOldData() {
        mealService.manualCleanup();
        return ResponseEntity.ok()
                .cacheControl(NO_CACHE)
                .body("이전 식단 데이터 정리가 완료되었습니다.");
    }
}