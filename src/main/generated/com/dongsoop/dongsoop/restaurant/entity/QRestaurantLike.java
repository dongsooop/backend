package com.dongsoop.dongsoop.restaurant.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QRestaurantLike is a Querydsl query type for RestaurantLike
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QRestaurantLike extends EntityPathBase<RestaurantLike> {

    private static final long serialVersionUID = -256577989L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QRestaurantLike restaurantLike = new QRestaurantLike("restaurantLike");

    public final QRestaurantLike_RestaurantLikeKey id;

    public QRestaurantLike(String variable) {
        this(RestaurantLike.class, forVariable(variable), INITS);
    }

    public QRestaurantLike(Path<? extends RestaurantLike> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QRestaurantLike(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QRestaurantLike(PathMetadata metadata, PathInits inits) {
        this(RestaurantLike.class, metadata, inits);
    }

    public QRestaurantLike(Class<? extends RestaurantLike> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.id = inits.isInitialized("id") ? new QRestaurantLike_RestaurantLikeKey(forProperty("id"), inits.get("id")) : null;
    }

}

