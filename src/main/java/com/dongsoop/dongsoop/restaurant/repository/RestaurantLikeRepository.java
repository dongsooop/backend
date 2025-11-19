package com.dongsoop.dongsoop.restaurant.repository;

import com.dongsoop.dongsoop.restaurant.entity.RestaurantLike;
import com.dongsoop.dongsoop.restaurant.entity.RestaurantLike.RestaurantLikeKey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantLikeRepository extends JpaRepository<RestaurantLike, RestaurantLikeKey> {
}