package com.dongsoop.dongsoop.restaurant.repository;

import com.dongsoop.dongsoop.restaurant.entity.RestaurantLike;
import com.dongsoop.dongsoop.restaurant.entity.RestaurantLike.RestaurantLikeKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RestaurantLikeRepository extends JpaRepository<RestaurantLike, RestaurantLikeKey> {

    @Query(value = "SELECT EXISTS(SELECT 1 FROM restaurant_like " + "WHERE restaurant_id = :restaurantId AND member_id = :memberId)",
            nativeQuery = true)
    boolean existsByRestaurantIdAndMemberId(@Param("restaurantId") Long restaurantId, @Param("memberId") Long memberId);
}