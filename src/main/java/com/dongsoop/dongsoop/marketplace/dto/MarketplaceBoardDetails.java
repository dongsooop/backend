package com.dongsoop.dongsoop.marketplace.dto;

import java.time.LocalDateTime;
import java.util.Arrays;

public record MarketplaceBoardDetails(
        Long id,
        String title,
        String content,
        Long price,
        LocalDateTime createdAt,
        Long contactCount,
        String[] imageUrlList,
        MarketplaceViewType viewType
) {
    public MarketplaceBoardDetails(Long id, String title, String content, Long price, LocalDateTime createdAt,
                                   Long contactCount, String imageUrls, MarketplaceViewType viewType) {
        this(id,
                title,
                content,
                price,
                createdAt,
                contactCount,
                splitImageUrl(imageUrls),
                viewType);
    }

    private static String[] splitImageUrl(String imageUrls) {
        return Arrays.stream(imageUrls.split(","))
                .map(String::trim)
                .toArray(String[]::new);
    }
}
