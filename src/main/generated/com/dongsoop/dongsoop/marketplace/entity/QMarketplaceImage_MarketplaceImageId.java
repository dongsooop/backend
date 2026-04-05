package com.dongsoop.dongsoop.marketplace.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QMarketplaceImage_MarketplaceImageId is a Querydsl query type for MarketplaceImageId
 */
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QMarketplaceImage_MarketplaceImageId extends BeanPath<MarketplaceImage.MarketplaceImageId> {

    private static final long serialVersionUID = 2136548174L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QMarketplaceImage_MarketplaceImageId marketplaceImageId = new QMarketplaceImage_MarketplaceImageId("marketplaceImageId");

    public final QMarketplaceBoard marketplaceBoard;

    public final StringPath url = createString("url");

    public QMarketplaceImage_MarketplaceImageId(String variable) {
        this(MarketplaceImage.MarketplaceImageId.class, forVariable(variable), INITS);
    }

    public QMarketplaceImage_MarketplaceImageId(Path<? extends MarketplaceImage.MarketplaceImageId> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QMarketplaceImage_MarketplaceImageId(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QMarketplaceImage_MarketplaceImageId(PathMetadata metadata, PathInits inits) {
        this(MarketplaceImage.MarketplaceImageId.class, metadata, inits);
    }

    public QMarketplaceImage_MarketplaceImageId(Class<? extends MarketplaceImage.MarketplaceImageId> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.marketplaceBoard = inits.isInitialized("marketplaceBoard") ? new QMarketplaceBoard(forProperty("marketplaceBoard"), inits.get("marketplaceBoard")) : null;
    }

}

