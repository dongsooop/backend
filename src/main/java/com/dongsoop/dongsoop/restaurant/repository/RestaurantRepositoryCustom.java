package com.dongsoop.dongsoop.restaurant.repository;

import com.dongsoop.dongsoop.restaurant.dto.RestaurantOverview;
import com.dongsoop.dongsoop.restaurant.entity.RestaurantCategory;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface RestaurantRepositoryCustom {
    List<RestaurantOverview> findNearbyRestaurants(Long memberId, RestaurantCategory category, Pageable pageable);

    boolean existsActiveByExternalMapId(String externalMapId);
}