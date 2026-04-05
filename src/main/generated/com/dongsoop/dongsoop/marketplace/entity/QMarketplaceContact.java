package com.dongsoop.dongsoop.marketplace.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QMarketplaceContact is a Querydsl query type for MarketplaceContact
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMarketplaceContact extends EntityPathBase<MarketplaceContact> {

    private static final long serialVersionUID = -445334160L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QMarketplaceContact marketplaceContact = new QMarketplaceContact("marketplaceContact");

    public final com.dongsoop.dongsoop.common.QBaseEntity _super = new com.dongsoop.dongsoop.common.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final QMarketplaceContact_MarketplaceContactId id;

    //inherited
    public final BooleanPath isDeleted = _super.isDeleted;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QMarketplaceContact(String variable) {
        this(MarketplaceContact.class, forVariable(variable), INITS);
    }

    public QMarketplaceContact(Path<? extends MarketplaceContact> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QMarketplaceContact(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QMarketplaceContact(PathMetadata metadata, PathInits inits) {
        this(MarketplaceContact.class, metadata, inits);
    }

    public QMarketplaceContact(Class<? extends MarketplaceContact> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.id = inits.isInitialized("id") ? new QMarketplaceContact_MarketplaceContactId(forProperty("id"), inits.get("id")) : null;
    }

}

