package com.dongsoop.dongsoop.marketplace.dto;

public record CreateMarketplaceBoardRequest(
        String title,
        String content,
        Long price
) {
}
