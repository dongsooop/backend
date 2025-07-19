package com.dongsoop.dongsoop.marketplace.dto;

import com.dongsoop.dongsoop.marketplace.entity.MarketplaceBoardStatus;
import com.dongsoop.dongsoop.marketplace.entity.MarketplaceType;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;

public record MarketplaceBoardDetails(
        Long id,
        Long authorId,
        String title,
        String content,
        Long price,
        LocalDateTime createdAt,
        MarketplaceType type,
        Long contactCount,
        Set<String> imageUrlList,
        MarketplaceViewType viewType,
        MarketplaceBoardStatus status
) {
    public MarketplaceBoardDetails(Long id, Long authorId, String title, String content, Long price,
                                   LocalDateTime createdAt,
                                   MarketplaceType type, Long contactCount, String imageUrls,
                                   MarketplaceViewType viewType, MarketplaceBoardStatus status) {
        this(id,
                authorId,
                title,
                content,
                price,
                createdAt,
                type,
                contactCount,
                splitImageUrl(imageUrls),
                viewType,
                status);
    }

    private static Set<String> splitImageUrl(String imageUrls) {
        if (!StringUtils.hasText(imageUrls)) {
            return Collections.emptySet();
        }

        return Arrays.stream(imageUrls.split(","))
                .map(String::trim)
                .collect(Collectors.toSet());
    }
}
