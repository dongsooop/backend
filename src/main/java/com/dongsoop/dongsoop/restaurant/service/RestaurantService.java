package com.dongsoop.dongsoop.restaurant.service;

import com.dongsoop.dongsoop.restaurant.dto.RestaurantOverview;
import com.dongsoop.dongsoop.restaurant.dto.RestaurantRegisterRequest;
import com.dongsoop.dongsoop.restaurant.entity.Restaurant;
import com.dongsoop.dongsoop.restaurant.entity.RestaurantCategory;
import com.dongsoop.dongsoop.restaurant.entity.RestaurantReportReason;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface RestaurantService {
  Restaurant registerRestaurant(RestaurantRegisterRequest request);

  List<RestaurantOverview> getNearbyRestaurants(RestaurantCategory category, Pageable pageable);

  void toggleLike(Long restaurantId, Long memberId, boolean isAdding);

  void createRestaurantReport(
      Long restaurantId, Long reporterId, RestaurantReportReason reason, String description);

  boolean checkDuplicateByExternalId(String externalMapId);
}
