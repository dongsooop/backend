package com.dongsoop.dongsoop.restaurant.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;

/** 좋아요 집계와 현재 조회 회원의 좋아요 여부. */
@Getter
public final class RestaurantStats {

    private final Long likeCount;
    private final Boolean isLikedByMe;

    @QueryProjection
    public RestaurantStats(Long likeCount, Boolean isLikedByMe) {
        this.likeCount = likeCount;
        this.isLikedByMe = isLikedByMe;
    }
}
