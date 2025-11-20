package com.dongsoop.dongsoop.restaurant.repository;

import com.dongsoop.dongsoop.restaurant.dto.RestaurantOverview;
import com.dongsoop.dongsoop.restaurant.entity.RestaurantStatus;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface RestaurantRepositoryCustom {
    List<RestaurantOverview> findNearbyRestaurants(Long memberId, Pageable pageable);

    List<RestaurantOverview> findRestaurantsByStatus(
            RestaurantStatus status, Pageable pageable, Long memberId);
}