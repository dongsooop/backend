package com.dongsoop.dongsoop.restaurant.service;

import com.dongsoop.dongsoop.restaurant.dto.RestaurantOverview;
import com.dongsoop.dongsoop.restaurant.dto.RestaurantRegisterRequest;
import com.dongsoop.dongsoop.restaurant.entity.Restaurant;
import com.dongsoop.dongsoop.restaurant.entity.RestaurantReportReason;
import com.dongsoop.dongsoop.restaurant.entity.RestaurantStatus;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface RestaurantService {
    Restaurant registerRestaurant(RestaurantRegisterRequest request);

    List<RestaurantOverview> getNearbyRestaurants(Long memberId, Pageable pageable);

    void addLike(Long restaurantId, Long memberId);

    void removeLike(Long restaurantId, Long memberId);

    List<RestaurantOverview> getRestaurantsByStatus(RestaurantStatus status, Long memberId, Pageable pageable);

    void approveRestaurant(Long restaurantId);

    void rejectRestaurant(Long restaurantId);

    void createRestaurantReport(Long restaurantId, Long reporterId, RestaurantReportReason reason, String description);
}