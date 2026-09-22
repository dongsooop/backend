package com.dongsoop.dongsoop.meal.dto;

import java.util.List;

public record MealPriceResponse(int ticketPrice, List<Category> categories) {

    public record Category(String name, String note, List<Item> items) {
    }

    public record Item(String name, int price, Integer largePrice, List<String> days) {
    }
}
