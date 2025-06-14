package com.dongsoop.dongsoop.marketplace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateMarketplaceBoardRequest(

        @NotBlank
        String title,

        @NotBlank
        String content,

        @NotNull
        @Positive
        Long price
) {
}
