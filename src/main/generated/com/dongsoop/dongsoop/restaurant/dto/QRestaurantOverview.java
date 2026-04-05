package com.dongsoop.dongsoop.restaurant.dto;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.ConstructorExpression;
import javax.annotation.processing.Generated;

/**
 * com.dongsoop.dongsoop.restaurant.dto.QRestaurantOverview is a Querydsl Projection type for RestaurantOverview
 */
@Generated("com.querydsl.codegen.DefaultProjectionSerializer")
public class QRestaurantOverview extends ConstructorExpression<RestaurantOverview> {

    private static final long serialVersionUID = 1408136105L;

    public QRestaurantOverview(com.querydsl.core.types.Expression<Long> id, com.querydsl.core.types.Expression<String> name, com.querydsl.core.types.Expression<com.dongsoop.dongsoop.restaurant.entity.RestaurantCategory> categoryEnum, com.querydsl.core.types.Expression<Double> distance, com.querydsl.core.types.Expression<String> placeUrl, com.querydsl.core.types.Expression<Long> likeCount, com.querydsl.core.types.Expression<String> tags, com.querydsl.core.types.Expression<String> externalMapId, com.querydsl.core.types.Expression<Boolean> isLikedByMe) {
        super(RestaurantOverview.class, new Class<?>[]{long.class, String.class, com.dongsoop.dongsoop.restaurant.entity.RestaurantCategory.class, double.class, String.class, long.class, String.class, String.class, boolean.class}, id, name, categoryEnum, distance, placeUrl, likeCount, tags, externalMapId, isLikedByMe);
    }

}

