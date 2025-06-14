package com.dongsoop.dongsoop.marketplace.dto;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;

public record MarketplaceBoardDetails(
        Long id,
        String title,
        String content,
        Long price,
        LocalDateTime createdAt,
        Long contactCount,
        Set<String> imageUrlList,
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

    private static Set<String> splitImageUrl(String imageUrls) {
        if (StringUtils.hasText(imageUrls)) {
            return Collections.emptySet();
        }

        return Arrays.stream(imageUrls.split(","))
                .map(String::trim)
                .collect(Collectors.toSet());
    }
}
