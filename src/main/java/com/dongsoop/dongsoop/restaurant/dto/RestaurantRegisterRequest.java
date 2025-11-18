package com.dongsoop.dongsoop.restaurant.dto;

import com.dongsoop.dongsoop.restaurant.entity.RestaurantCategory;
import com.dongsoop.dongsoop.restaurant.entity.RestaurantTag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.Length;

import java.util.List;

public record RestaurantRegisterRequest(
        @NotBlank
        String externalMapId,

        @NotBlank
        @Length(max = 255)
        String name,

        @NotBlank
        @Length(max = 512)
        String address,

        @Length(max = 512)
        String placeUrl,

        @NotNull
        Double latitude,

        @NotNull
        Double longitude,

        @NotNull
        RestaurantCategory category,

        @Length(max = 20)
        String phone,

        @Size(max = 3, message = "태그는 최대 3개까지 선택 가능합니다.")
        List<RestaurantTag> tags
) {
}