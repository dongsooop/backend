package com.dongsoop.dongsoop.marketplace.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QMarketplaceContact_MarketplaceContactId is a Querydsl query type for MarketplaceContactId
 */
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QMarketplaceContact_MarketplaceContactId extends BeanPath<MarketplaceContact.MarketplaceContactId> {

    private static final long serialVersionUID = -503908050L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QMarketplaceContact_MarketplaceContactId marketplaceContactId = new QMarketplaceContact_MarketplaceContactId("marketplaceContactId");

    public final com.dongsoop.dongsoop.member.entity.QMember applicant;

    public final NumberPath<Long> marketplaceId = createNumber("marketplaceId", Long.class);

    public QMarketplaceContact_MarketplaceContactId(String variable) {
        this(MarketplaceContact.MarketplaceContactId.class, forVariable(variable), INITS);
    }

    public QMarketplaceContact_MarketplaceContactId(Path<? extends MarketplaceContact.MarketplaceContactId> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QMarketplaceContact_MarketplaceContactId(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QMarketplaceContact_MarketplaceContactId(PathMetadata metadata, PathInits inits) {
        this(MarketplaceContact.MarketplaceContactId.class, metadata, inits);
    }

    public QMarketplaceContact_MarketplaceContactId(Class<? extends MarketplaceContact.MarketplaceContactId> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.applicant = inits.isInitialized("applicant") ? new com.dongsoop.dongsoop.member.entity.QMember(forProperty("applicant"), inits.get("applicant")) : null;
    }

}

