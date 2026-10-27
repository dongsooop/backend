package com.dongsoop.dongsoop.restaurant.dto;

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
    private final Long likeCount;
    private final Boolean isLikedByMe;
    private final List<String> tags;
    private final String externalMapId;
    private final String placeUrl;

    @QueryProjection
    public RestaurantOverview(RestaurantInfo info, RestaurantStats stats, String tags) {
        this.id = info.id();
        this.name = info.name();
        this.category = info.category().getDisplayName();
        this.distance = info.distance();
        this.likeCount = stats.likeCount();
        this.isLikedByMe = stats.isLikedByMe();
        this.tags = parseTags(tags);
        this.externalMapId = info.externalMapId();
        this.placeUrl = info.placeUrl();
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
