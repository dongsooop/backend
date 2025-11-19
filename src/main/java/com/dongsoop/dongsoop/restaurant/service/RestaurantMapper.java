package com.dongsoop.dongsoop.restaurant.service;

import com.dongsoop.dongsoop.restaurant.dto.RestaurantRegisterRequest;
import com.dongsoop.dongsoop.restaurant.entity.Restaurant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RestaurantMapper {

    public Restaurant toEntity(RestaurantRegisterRequest request) {
        return Restaurant.builder()
                .externalMapId(request.externalMapId())
                .name(request.name())
                .category(request.category())
                .placeUrl(request.placeUrl())
                .tags(request.tags())
                .distance(request.distance())
                .build();
    }
}