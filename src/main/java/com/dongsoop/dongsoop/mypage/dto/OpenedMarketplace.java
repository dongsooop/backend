package com.dongsoop.dongsoop.mypage.dto;

import com.dongsoop.dongsoop.marketplace.entity.MarketplaceBoardStatus;
import com.dongsoop.dongsoop.marketplace.entity.MarketplaceType;
import java.time.LocalDateTime;

public record OpenedMarketplace(

        Long id,
        String title,
        String content,
        Long price,
        LocalDateTime createdAt,
        Long contactCount,
        String imageUrl,
        MarketplaceType type,
        MarketplaceBoardStatus status
) {
}
