package com.dongsoop.dongsoop.search.dto;

import com.dongsoop.dongsoop.search.entity.RestaurantDocument;
import lombok.Builder;
import lombok.Getter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Getter
@Builder
public class RestaurantSearchResult {
    private String id;
    private Long restaurantId;
    private String name;
    private String category;
    private List<String> tags;
    private String placeUrl;
    private Integer likeCount;
    private Double distance;
    private String externalMapId;

    public static RestaurantSearchResult from(RestaurantDocument doc) {
        return RestaurantSearchResult.builder()
                .id(doc.getId())
                .restaurantId(doc.getRestaurantId())
                .name(doc.getName())
                .category(doc.getCategory())
                .tags(parseTags(doc.getTags()))
                .placeUrl(doc.getPlaceUrl())
                .likeCount(doc.getLikeCount())
                .distance(doc.getDistance())
                .externalMapId(doc.getExternalMapId())
                .build();
    }

    private static List<String> parseTags(String tags) {
        if (tags == null || tags.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .toList();
    }
}