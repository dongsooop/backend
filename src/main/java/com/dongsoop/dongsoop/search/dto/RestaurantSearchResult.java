package com.dongsoop.dongsoop.search.dto;

import com.dongsoop.dongsoop.restaurant.entity.RestaurantCategory;
import com.dongsoop.dongsoop.search.entity.RestaurantDocument;
import lombok.Builder;
import lombok.Getter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Getter
@Builder
public class RestaurantSearchResult {
    private Long id;
    private Long restaurantId;
    private String name;
    private String category;
    private List<String> tags;
    private String placeUrl;
    private Integer likeCount;
    private Boolean isLikedByMe;
    private Double distance;
    private Long externalMapId;

    public static RestaurantSearchResult from(RestaurantDocument doc, Long currentMemberId) {
        return RestaurantSearchResult.builder()
                .id(doc.getRestaurantId())
                .restaurantId(doc.getRestaurantId())
                .name(doc.getName())
                .category(validateAndGetDisplayName(doc.getCategory()))
                .tags(parseTags(doc.getTags()))
                .placeUrl(doc.getPlaceUrl())
                .likeCount(doc.getLikeCount())
                .distance(doc.getDistance())
                .externalMapId(Long.parseLong(doc.getExternalMapId()))
                .isLikedByMe(checkIsLiked(doc.getLikedMemberIds(), currentMemberId))
                .build();
    }

    private static String validateAndGetDisplayName(String categoryName) {
        try {
            return RestaurantCategory.valueOf(categoryName).getDisplayName();
        } catch (IllegalArgumentException | NullPointerException e) {
            return "기타";
        }
    }

    private static boolean checkIsLiked(String likedMemberIdsStr, Long currentMemberId) {
        if (currentMemberId == null || likedMemberIdsStr == null || likedMemberIdsStr.isEmpty()) {
            return false;
        }
        String targetId = String.valueOf(currentMemberId);
        return Arrays.asList(likedMemberIdsStr.split(",")).contains(targetId);
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