package com.dongsoop.dongsoop.meal.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MealType {
    KOREAN("한식"),
    SPECIAL("단품");

    private final String description;
}