package com.dongsoop.dongsoop.restaurant.dto;

import com.dongsoop.dongsoop.restaurant.entity.RestaurantCategory;
import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;

/** 식당 자체의 기본 조회 정보. 회원별 좋아요와 집계 결과는 포함하지 않는다. */
@Getter
public final class RestaurantInfo {

    private final Long id;
    private final String name;
    private final RestaurantCategory category;
    private final Double distance;
    private final String placeUrl;
    private final String externalMapId;

    @QueryProjection
    public RestaurantInfo(Long id, String name, RestaurantCategory category, Double distance,
                          String placeUrl, String externalMapId) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.distance = distance;
        this.placeUrl = placeUrl;
        this.externalMapId = externalMapId;
    }
}
