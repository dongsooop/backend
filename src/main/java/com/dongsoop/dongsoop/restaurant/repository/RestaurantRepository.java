package com.dongsoop.dongsoop.restaurant.repository;

import com.dongsoop.dongsoop.restaurant.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long>, RestaurantRepositoryCustom {
    boolean existsByExternalMapId(String externalMapId);

    @Query("SELECT COUNT(r) > 0 FROM Restaurant r WHERE r.externalMapId = :externalMapId AND r.isDeleted = false")
    boolean existsActiveByExternalMapId(@Param("externalMapId") String externalMapId);
}