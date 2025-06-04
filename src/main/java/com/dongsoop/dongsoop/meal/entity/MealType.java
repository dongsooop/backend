package com.dongsoop.dongsoop.meal.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MealType {
    KOREAN("한식"),
    SPECIAL("별미");

    private final String description;
}