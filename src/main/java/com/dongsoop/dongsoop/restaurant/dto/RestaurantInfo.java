package com.dongsoop.dongsoop.restaurant.dto;

import com.dongsoop.dongsoop.restaurant.entity.RestaurantCategory;
import com.querydsl.core.annotations.QueryProjection;

/** 식당 자체의 기본 조회 정보. 회원별 좋아요와 집계 결과는 포함하지 않는다. */
public record RestaurantInfo(
        Long id,
        String name,
        RestaurantCategory category,
        Double distance,
        String placeUrl,
        String externalMapId
) {
    @QueryProjection
    public RestaurantInfo {
        // @QueryProjection을 위해 생성자를 선언하며, 필드 대입은 record가 자동으로 수행한다.
    }
}
