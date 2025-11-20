package com.dongsoop.dongsoop.restaurant.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RestaurantCategory {
    KOREAN("한식"),
    CHINESE("중식"),
    JAPANESE("일식"),
    WESTERN("양식"),
    BUNSIK("분식"),
    FAST_FOOD("패스트푸드"),
    CAFE_DESSERT("카페/디저트");

    private final String displayName;
}