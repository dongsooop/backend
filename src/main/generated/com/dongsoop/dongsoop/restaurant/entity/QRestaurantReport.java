package com.dongsoop.dongsoop.restaurant.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QRestaurantReport is a Querydsl query type for RestaurantReport
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QRestaurantReport extends EntityPathBase<RestaurantReport> {

    private static final long serialVersionUID = -1590068520L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QRestaurantReport restaurantReport = new QRestaurantReport("restaurantReport");

    public final com.dongsoop.dongsoop.common.QBaseEntity _super = new com.dongsoop.dongsoop.common.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final StringPath description = createString("description");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final BooleanPath isDeleted = _super.isDeleted;

    public final EnumPath<RestaurantReportReason> reason = createEnum("reason", RestaurantReportReason.class);

    public final com.dongsoop.dongsoop.member.entity.QMember reporter;

    public final QRestaurant restaurant;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QRestaurantReport(String variable) {
        this(RestaurantReport.class, forVariable(variable), INITS);
    }

    public QRestaurantReport(Path<? extends RestaurantReport> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QRestaurantReport(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QRestaurantReport(PathMetadata metadata, PathInits inits) {
        this(RestaurantReport.class, metadata, inits);
    }

    public QRestaurantReport(Class<? extends RestaurantReport> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.reporter = inits.isInitialized("reporter") ? new com.dongsoop.dongsoop.member.entity.QMember(forProperty("reporter"), inits.get("reporter")) : null;
        this.restaurant = inits.isInitialized("restaurant") ? new QRestaurant(forProperty("restaurant")) : null;
    }

}

