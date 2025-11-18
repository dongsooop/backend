package com.dongsoop.dongsoop.restaurant.repository;

import com.dongsoop.dongsoop.restaurant.dto.QRestaurantOverview;
import com.dongsoop.dongsoop.restaurant.dto.RestaurantOverview;
import com.dongsoop.dongsoop.restaurant.entity.QRestaurant;
import com.dongsoop.dongsoop.restaurant.entity.QRestaurantLike;
import com.dongsoop.dongsoop.restaurant.entity.RestaurantStatus;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RestaurantRepositoryCustomImpl implements RestaurantRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final QRestaurant restaurant = QRestaurant.restaurant;
    private final QRestaurantLike restaurantLike = QRestaurantLike.restaurantLike;

    @Override
    public List<RestaurantOverview> findNearbyRestaurants(
            double latitude, double longitude, double distanceKm, Long memberId, Pageable pageable) {

        NumberExpression<Double> distanceExpression = Expressions.numberTemplate(Double.class,
                "6371 * acos(cos(radians({0})) * cos(radians(restaurant.latitude)) * " +
                        "cos(radians(restaurant.longitude) - radians({1})) + " +
                        "sin(radians({0})) * sin(radians(restaurant.latitude)))",
                latitude, longitude
        );

        BooleanExpression isLikedByMe = Optional.ofNullable(memberId)
                .map(id -> JPAExpressions
                        .selectOne()
                        .from(restaurantLike)
                        .where(restaurantLike.id.restaurant.id.eq(restaurant.id),
                                restaurantLike.id.member.id.eq(id))
                        .exists()
                )
                .orElse(Expressions.asBoolean(false));

        return queryFactory
                .select(new QRestaurantOverview(
                        restaurant.id,
                        restaurant.name,
                        restaurant.phone,
                        restaurant.category,
                        restaurant.latitude,
                        restaurant.longitude,
                        JPAExpressions
                                .select(restaurantLike.count())
                                .from(restaurantLike)
                                .where(restaurantLike.id.restaurant.id.eq(restaurant.id)),
                        isLikedByMe,
                        restaurant.tags,
                        restaurant.externalMapId
                ))
                .from(restaurant)
                .where(
                        distanceExpression.loe(distanceKm),
                        restaurant.status.eq(RestaurantStatus.APPROVED)
                )
                .orderBy(distanceExpression.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }

    @Override
    public List<RestaurantOverview> findRestaurantsByStatus(
            RestaurantStatus status, Pageable pageable, Long memberId) {

        BooleanExpression isLikedByMe = Optional.ofNullable(memberId)
                .map(id -> JPAExpressions
                        .selectOne()
                        .from(restaurantLike)
                        .where(restaurantLike.id.restaurant.id.eq(restaurant.id),
                                restaurantLike.id.member.id.eq(id))
                        .exists()
                )
                .orElse(Expressions.asBoolean(false));

        return queryFactory
                .select(new QRestaurantOverview(
                        restaurant.id,
                        restaurant.name,
                        restaurant.phone,
                        restaurant.category,
                        restaurant.latitude,
                        restaurant.longitude,
                        JPAExpressions
                                .select(restaurantLike.count())
                                .from(restaurantLike)
                                .where(restaurantLike.id.restaurant.id.eq(restaurant.id)),
                        isLikedByMe,
                        restaurant.tags,
                        restaurant.externalMapId
                ))
                .from(restaurant)
                .where(restaurant.status.eq(status))
                .orderBy(restaurant.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }
}