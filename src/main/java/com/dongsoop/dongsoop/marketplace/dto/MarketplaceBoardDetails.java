package com.dongsoop.dongsoop.marketplace.dto;

import java.time.LocalDateTime;

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
                                   Long contactCount, String imageUrlList, MarketplaceViewType viewType) {
        this(id, title, content, price, createdAt, contactCount, imageUrlList.split(","), viewType);
    }
}
