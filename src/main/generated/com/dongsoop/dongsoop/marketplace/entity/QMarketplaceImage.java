package com.dongsoop.dongsoop.marketplace.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QMarketplaceImage is a Querydsl query type for MarketplaceImage
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMarketplaceImage extends EntityPathBase<MarketplaceImage> {

    private static final long serialVersionUID = -1594992981L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QMarketplaceImage marketplaceImage = new QMarketplaceImage("marketplaceImage");

    public final com.dongsoop.dongsoop.common.QBaseEntity _super = new com.dongsoop.dongsoop.common.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final QMarketplaceImage_MarketplaceImageId id;

    //inherited
    public final BooleanPath isDeleted = _super.isDeleted;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QMarketplaceImage(String variable) {
        this(MarketplaceImage.class, forVariable(variable), INITS);
    }

    public QMarketplaceImage(Path<? extends MarketplaceImage> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QMarketplaceImage(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QMarketplaceImage(PathMetadata metadata, PathInits inits) {
        this(MarketplaceImage.class, metadata, inits);
    }

    public QMarketplaceImage(Class<? extends MarketplaceImage> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.id = inits.isInitialized("id") ? new QMarketplaceImage_MarketplaceImageId(forProperty("id"), inits.get("id")) : null;
    }

}

