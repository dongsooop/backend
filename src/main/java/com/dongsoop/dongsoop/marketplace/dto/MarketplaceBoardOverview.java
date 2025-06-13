package com.dongsoop.dongsoop.marketplace.dto;

import java.time.LocalDateTime;

public record MarketplaceBoardOverview(

        Long id,
        String title,
        String content,
        Long price,
        LocalDateTime createdAt,
        Long contactCount,
        String imageUrl
) {
}
