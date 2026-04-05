package com.dongsoop.dongsoop.restaurant.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QRestaurant is a Querydsl query type for Restaurant
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QRestaurant extends EntityPathBase<Restaurant> {

    private static final long serialVersionUID = -2105365372L;

    public static final QRestaurant restaurant = new QRestaurant("restaurant");

    public final com.dongsoop.dongsoop.common.QBaseEntity _super = new com.dongsoop.dongsoop.common.QBaseEntity(this);

    public final EnumPath<RestaurantCategory> category = createEnum("category", RestaurantCategory.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Double> distance = createNumber("distance", Double.class);

    public final StringPath externalMapId = createString("externalMapId");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final BooleanPath isDeleted = _super.isDeleted;

    public final StringPath name = createString("name");

    public final StringPath placeUrl = createString("placeUrl");

    public final ListPath<RestaurantTag, EnumPath<RestaurantTag>> tags = this.<RestaurantTag, EnumPath<RestaurantTag>>createList("tags", RestaurantTag.class, EnumPath.class, PathInits.DIRECT2);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QRestaurant(String variable) {
        super(Restaurant.class, forVariable(variable));
    }

    public QRestaurant(Path<? extends Restaurant> path) {
        super(path.getType(), path.getMetadata());
    }

    public QRestaurant(PathMetadata metadata) {
        super(Restaurant.class, metadata);
    }

}

