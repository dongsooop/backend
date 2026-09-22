package com.dongsoop.dongsoop.meal.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

// 학교 페이지에는 가격이 없어 설정 파일로 관리한다 (application.yml 의 meal.prices)
@ConfigurationProperties(prefix = "meal.prices")
public record MealPriceProperties(int ticketPrice, List<Category> categories) {

    public record Category(String name, String note, List<Item> items) {
    }

    public record Item(String name, int price, Integer largePrice, List<String> days) {
    }
}
