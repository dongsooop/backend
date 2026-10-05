package com.dongsoop.dongsoop.meal.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

// meal-prices.yml 의 가격표가 깨지지 않았는지 확인한다. 값을 고칠 때 이 테스트도 같이 맞춘다
class MealPricePropertiesTest {

    @Test
    @DisplayName("meal-prices.yml 의 meal.prices 가 식권 가격과 네 분류로 바인딩된다")
    void bindsPricesFromMealPricesYml() {
        MealPriceProperties properties = bind();

        assertEquals(6500, properties.ticketPrice());
        assertEquals(List.of("돈까스류", "라면류", "덮밥류", "음료류"),
                properties.categories().stream().map(MealPriceProperties.Category::name).toList());
    }

    @Test
    @DisplayName("곱빼기 가격과 판매 요일이 있는 항목은 그 값이, 없는 항목은 비어 있다")
    void bindsLargePriceAndDays() {
        MealPriceProperties properties = bind();

        MealPriceProperties.Item pork = item(properties, "덮밥류", "삼겹살덮밥");
        assertEquals(5500, pork.price());
        assertEquals(7500, pork.largePrice());
        assertEquals(List.of("수"), pork.days());

        MealPriceProperties.Item ramen = item(properties, "라면류", "신라면");
        assertEquals(3500, ramen.price());
        assertNull(ramen.largePrice());
        assertTrue(ramen.days() == null || ramen.days().isEmpty());
    }

    // application.yml 의 spring.config.import 를 거쳐 읽히는지까지 확인하려고 실제 설정 로딩으로 띄운다
    private MealPriceProperties bind() {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(MealPriceConfig.class)
                .web(WebApplicationType.NONE)
                .run()) {
            return context.getBean(MealPriceProperties.class);
        }
    }

    private MealPriceProperties.Item item(MealPriceProperties properties, String categoryName, String itemName) {
        return properties.categories().stream()
                .filter(category -> category.name().equals(categoryName))
                .flatMap(category -> category.items().stream())
                .filter(item -> item.name().equals(itemName))
                .findFirst()
                .orElseThrow();
    }
}
