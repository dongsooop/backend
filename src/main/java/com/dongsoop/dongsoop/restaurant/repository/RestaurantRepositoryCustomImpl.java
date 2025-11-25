package com.dongsoop.dongsoop.restaurant.repository;

import com.dongsoop.dongsoop.restaurant.dto.QRestaurantOverview;
import com.dongsoop.dongsoop.restaurant.dto.RestaurantOverview;
import com.dongsoop.dongsoop.restaurant.entity.QRestaurant;
import com.dongsoop.dongsoop.restaurant.entity.QRestaurantLike;
import com.dongsoop.dongsoop.restaurant.entity.RestaurantTag;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.EnumPath;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class RestaurantRepositoryCustomImpl implements RestaurantRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final QRestaurant restaurant = QRestaurant.restaurant;
    private final QRestaurantLike restaurantLike = QRestaurantLike.restaurantLike;

    private final EnumPath<RestaurantTag> tagAlias = Expressions.enumPath(RestaurantTag.class, "T");

    @Override
    public List<RestaurantOverview> findNearbyRestaurants(Long memberId, Pageable pageable) {
        return queryFactory
                .select(new QRestaurantOverview(
                        restaurant.id,
                        restaurant.name,
                        restaurant.category,
                        restaurant.distance,
                        getLikeCountSubQuery(),
                        Expressions.stringTemplate("STRING_AGG({0}, ',')", tagAlias),
                        restaurant.externalMapId,
                        isLikedByMember(memberId)
                ))
                .from(restaurant)
                .leftJoin(restaurant.tags, tagAlias)
                .where(restaurant.isDeleted.isFalse())
                .groupBy(
                        restaurant.id, restaurant.name, restaurant.category,
                        restaurant.distance, restaurant.externalMapId
                )
                .orderBy(restaurant.distance.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }

    @Override
    public boolean existsActiveByExternalMapId(String externalMapId) {
        Integer fetchOne = queryFactory
                .selectOne()
                .from(restaurant)
                .where(restaurant.externalMapId.eq(externalMapId)
                        .and(restaurant.isDeleted.isFalse()))
                .fetchFirst();
        return fetchOne != null;
    }

    private BooleanExpression isLikedByMember(Long memberId) {
        if (memberId == null) {
            return Expressions.asBoolean(false);
        }

        return JPAExpressions
                .selectOne()
                .from(restaurantLike)
                .where(restaurantLike.id.restaurant.id.eq(restaurant.id),
                        restaurantLike.id.member.id.eq(memberId))
                .exists();
    }

    private com.querydsl.core.types.Expression<Long> getLikeCountSubQuery() {
        return ExpressionUtils.as(
                JPAExpressions.select(restaurantLike.count())
                        .from(restaurantLike)
                        .where(restaurantLike.id.restaurant.id.eq(restaurant.id)),
                "likeCount"
        );
    }
}