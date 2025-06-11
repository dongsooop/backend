package com.dongsoop.dongsoop.marketplace.dto;

public record ApplyMarketplaceRequest(

        Long boardId,
        String introduction,
        String motivation
) {
}
