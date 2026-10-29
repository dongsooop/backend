package com.dongsoop.dongsoop.restaurant.dto;

import com.querydsl.core.annotations.QueryProjection;

/** 좋아요 집계와 현재 조회 회원의 좋아요 여부. */
public record RestaurantStats(Long likeCount, Boolean isLikedByMe) {
  @QueryProjection
  public RestaurantStats {
    // @QueryProjection을 위해 생성자를 선언하며, 필드 대입은 record가 자동으로 수행한다.
  }
}
