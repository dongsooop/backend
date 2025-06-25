package com.dongsoop.dongsoop.marketplace.dto;

import com.dongsoop.dongsoop.marketplace.entity.MarketplaceType;
import java.time.LocalDateTime;

public record MarketplaceBoardOverview(

        Long id,
        String title,
        String content,
        Long price,
        LocalDateTime createdAt,
        Long contactCount,
        String imageUrl,
        MarketplaceType type
) {
}
