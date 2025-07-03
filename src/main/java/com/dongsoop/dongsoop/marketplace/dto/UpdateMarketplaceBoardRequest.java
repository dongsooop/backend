package com.dongsoop.dongsoop.marketplace.dto;

import com.dongsoop.dongsoop.marketplace.entity.MarketplaceType;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record UpdateMarketplaceBoardRequest(

        String title,
        String content,

        @Positive
        Long price,
        List<String> deleteImageUrls,
        MarketplaceType type
) {
}
