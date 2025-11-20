package com.dongsoop.dongsoop.restaurant.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RestaurantTag {
    LARGE_PORTION("양이 많아요", "LARGE_PORTION"),
    DELICIOUS("음식이 맛있어요", "DELICIOUS"),
    GOOD_FOR_LUNCH("점심으로 괜찮아요", "GOOD_FOR_LUNCH"),
    GOOD_VALUE("가성비가 좋아요", "GOOD_VALUE"),
    GOOD_FOR_GATHERING("회식하기 좋아요", "GOOD_FOR_GATHERING"),
    GOOD_FOR_CONVERSATION("대화하기 좋아요", "GOOD_FOR_CONVERSATION");

    private final String description;
    private final String code;
}
