package com.dongsoop.dongsoop.mypage.dto;

import com.dongsoop.dongsoop.marketplace.entity.MarketplaceBoardStatus;
import com.dongsoop.dongsoop.marketplace.entity.MarketplaceType;
import java.time.LocalDateTime;
import com.dongsoop.dongsoop.marketplace.dto.MarketplaceSummary;

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
    public OpenedMarketplace(MarketplaceSummary summary) {
        this(summary.id(), summary.title(), summary.content(), summary.price(), summary.createdAt(),
                summary.contactCount(), summary.imageUrl(), summary.type(), summary.status());
    }
}
