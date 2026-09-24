package com.dongsoop.dongsoop.meal.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MealPriceProperties.class)
public class MealPriceConfig {
}
