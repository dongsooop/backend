package com.dongsoop.dongsoop.restaurant.repository;

import com.dongsoop.dongsoop.restaurant.dto.QRestaurantOverview;
import com.dongsoop.dongsoop.restaurant.dto.RestaurantOverview;
import com.dongsoop.dongsoop.restaurant.entity.QRestaurant;
import com.dongsoop.dongsoop.restaurant.entity.QRestaurantLike;
import com.dongsoop.dongsoop.restaurant.entity.RestaurantStatus;
import com.dongsoop.dongsoop.restaurant.entity.RestaurantTag;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.EnumPath;
import com.querydsl.core.types.dsl.Expressions;
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

    private final EnumPath<RestaurantTag> tagAlias = Expressions.enumPath(RestaurantTag.class, "T");

    @Override
    public List<RestaurantOverview> findNearbyRestaurants(Long memberId, Pageable pageable) {

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
                        restaurant.distance,
                        restaurant.likeCount,
                        Expressions.stringTemplate("STRING_AGG({0}, ',')", tagAlias),
                        restaurant.externalMapId,
                        isLikedByMe
                ))
                .from(restaurant)
                .leftJoin(restaurant.tags, tagAlias)
                .where(
                        restaurant.status.eq(RestaurantStatus.APPROVED)
                )
                .groupBy(
                        restaurant.id, restaurant.name, restaurant.phone, restaurant.category,
                        restaurant.distance, restaurant.likeCount, restaurant.externalMapId
                )
                .orderBy(restaurant.distance.asc())
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
                        restaurant.distance,
                        restaurant.likeCount,
                        Expressions.stringTemplate("STRING_AGG({0}, ',')", tagAlias),
                        restaurant.externalMapId,
                        isLikedByMe
                ))
                .from(restaurant)
                .leftJoin(restaurant.tags, tagAlias)
                .where(restaurant.status.eq(status))
                .groupBy(
                        restaurant.id, restaurant.name, restaurant.phone, restaurant.category,
                        restaurant.distance, restaurant.likeCount, restaurant.externalMapId
                )
                .orderBy(restaurant.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }
}