package com.dongsoop.dongsoop.restaurant.service;

import com.dongsoop.dongsoop.restaurant.dto.RestaurantOverview;
import com.dongsoop.dongsoop.restaurant.dto.RestaurantRegisterRequest;
import com.dongsoop.dongsoop.restaurant.entity.Restaurant;
import com.dongsoop.dongsoop.restaurant.entity.RestaurantReportReason;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface RestaurantService {
    Restaurant registerRestaurant(RestaurantRegisterRequest request);

    List<RestaurantOverview> getNearbyRestaurants(Pageable pageable);

    void toggleLike(Long restaurantId, Long memberId, boolean isAdding);

    void createRestaurantReport(Long restaurantId, Long reporterId, RestaurantReportReason reason, String description);

    boolean checkDuplicateByExternalId(String externalMapId);
}