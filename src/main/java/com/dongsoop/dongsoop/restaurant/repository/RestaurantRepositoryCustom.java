package com.dongsoop.dongsoop.restaurant.repository;

import com.dongsoop.dongsoop.restaurant.dto.RestaurantOverview;
import com.dongsoop.dongsoop.restaurant.entity.RestaurantCategory;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface RestaurantRepositoryCustom {
  List<RestaurantOverview> findNearbyRestaurants(
      Long memberId, RestaurantCategory category, Pageable pageable);

  boolean existsActiveByExternalMapId(String externalMapId);
}
