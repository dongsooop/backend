package com.dongsoop.dongsoop.restaurant.service;

import com.dongsoop.dongsoop.restaurant.dto.RestaurantRegisterRequest;
import com.dongsoop.dongsoop.restaurant.entity.Restaurant;
import com.dongsoop.dongsoop.restaurant.entity.RestaurantTag;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RestaurantMapper {

    public Restaurant toEntity(RestaurantRegisterRequest request) {
        return Restaurant.builder()
                .externalMapId(request.externalMapId())
                .name(request.name())
                .address(request.address())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .category(request.category())
                .placeUrl(request.placeUrl())
                .phone(request.phone())
                .tags(tagsToString(request.tags()))
                .build();
    }

    private String tagsToString(List<RestaurantTag> tags) {
        if (tags == null || tags.isEmpty()) {
            return "";
        }
        return tags.stream()
                .map(Enum::name)
                .collect(Collectors.joining(","));
    }
}