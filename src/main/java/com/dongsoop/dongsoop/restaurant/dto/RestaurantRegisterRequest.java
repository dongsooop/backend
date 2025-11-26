package com.dongsoop.dongsoop.restaurant.dto;

import com.dongsoop.dongsoop.restaurant.entity.RestaurantCategory;
import com.dongsoop.dongsoop.restaurant.entity.RestaurantTag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.Length;

import java.util.List;

public record RestaurantRegisterRequest(
        @NotBlank
        String externalMapId,

        @NotBlank
        @Length(max = 255)
        String name,

        @NotNull
        @Length(max = 512)
        String placeUrl,

        @NotNull(message = "거리는 필수 입력값입니다.")
        @Positive(message = "거리는 양수여야 합니다.")
        Double distance,

        @NotNull
        RestaurantCategory category,

        @Size(max = 3, message = "태그는 최대 3개까지 선택 가능합니다.")
        List<RestaurantTag> tags
) {
}