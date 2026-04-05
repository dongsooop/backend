package com.dongsoop.dongsoop.restaurant.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QRestaurantLike_RestaurantLikeKey is a Querydsl query type for RestaurantLikeKey
 */
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QRestaurantLike_RestaurantLikeKey extends BeanPath<RestaurantLike.RestaurantLikeKey> {

    private static final long serialVersionUID = 1285632056L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QRestaurantLike_RestaurantLikeKey restaurantLikeKey = new QRestaurantLike_RestaurantLikeKey("restaurantLikeKey");

    public final com.dongsoop.dongsoop.member.entity.QMember member;

    public final QRestaurant restaurant;

    public QRestaurantLike_RestaurantLikeKey(String variable) {
        this(RestaurantLike.RestaurantLikeKey.class, forVariable(variable), INITS);
    }

    public QRestaurantLike_RestaurantLikeKey(Path<? extends RestaurantLike.RestaurantLikeKey> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QRestaurantLike_RestaurantLikeKey(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QRestaurantLike_RestaurantLikeKey(PathMetadata metadata, PathInits inits) {
        this(RestaurantLike.RestaurantLikeKey.class, metadata, inits);
    }

    public QRestaurantLike_RestaurantLikeKey(Class<? extends RestaurantLike.RestaurantLikeKey> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.member = inits.isInitialized("member") ? new com.dongsoop.dongsoop.member.entity.QMember(forProperty("member"), inits.get("member")) : null;
        this.restaurant = inits.isInitialized("restaurant") ? new QRestaurant(forProperty("restaurant")) : null;
    }

}

