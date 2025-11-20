package com.dongsoop.dongsoop.restaurant.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RestaurantReportReason {
    STORE_CLOSED("가게 폐업"),
    WRONG_INFORMATION("잘못된 정보"),
    OTHER("기타");

    private final String description;
}