package com.dongsoop.dongsoop.restaurant.dto;

import com.dongsoop.dongsoop.restaurant.entity.RestaurantCategory;
import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Getter
public final class RestaurantOverview {
    private final Long id;
    private final String name;
    private final String category;
    private final Double distance;
    private final long likeCount;
    private final boolean isLikedByMe;
    private final List<String> tags;
    private final String externalMapId;

    @QueryProjection
    public RestaurantOverview(Long id, String name, RestaurantCategory categoryEnum,
                              Double distance,
                              long likeCount, String tags,
                              String externalMapId, boolean isLikedByMe) {
        this.id = id;
        this.name = name;
        this.category = categoryEnum.getDisplayName();
        this.distance = distance;
        this.likeCount = likeCount;
        this.isLikedByMe = isLikedByMe;
        this.tags = parseTags(tags);
        this.externalMapId = externalMapId;
    }

    private static List<String> parseTags(String tags) {
        if (tags == null) {
            return Collections.emptyList();
        }

        if (tags.trim().isEmpty()) {
            return Collections.emptyList();
        }

        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}