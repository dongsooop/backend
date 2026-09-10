package com.dongsoop.dongsoop.eclass.dto;

import jakarta.validation.constraints.NotBlank;

public record EclassLinkRequest(

        @NotBlank
        String token
) {
}
