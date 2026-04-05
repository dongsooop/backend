package com.dongsoop.dongsoop.marketplace.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QMarketplaceBoard is a Querydsl query type for MarketplaceBoard
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMarketplaceBoard extends EntityPathBase<MarketplaceBoard> {

    private static final long serialVersionUID = -1601397706L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QMarketplaceBoard marketplaceBoard = new QMarketplaceBoard("marketplaceBoard");

    public final com.dongsoop.dongsoop.board.QBoard _super;

    // inherited
    public final com.dongsoop.dongsoop.member.entity.QMember author;

    //inherited
    public final StringPath content;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final BooleanPath isDeleted;

    public final NumberPath<Long> price = createNumber("price", Long.class);

    public final EnumPath<MarketplaceBoardStatus> status = createEnum("status", MarketplaceBoardStatus.class);

    //inherited
    public final StringPath title;

    public final EnumPath<MarketplaceType> type = createEnum("type", MarketplaceType.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt;

    public QMarketplaceBoard(String variable) {
        this(MarketplaceBoard.class, forVariable(variable), INITS);
    }

    public QMarketplaceBoard(Path<? extends MarketplaceBoard> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QMarketplaceBoard(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QMarketplaceBoard(PathMetadata metadata, PathInits inits) {
        this(MarketplaceBoard.class, metadata, inits);
    }

    public QMarketplaceBoard(Class<? extends MarketplaceBoard> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this._super = new com.dongsoop.dongsoop.board.QBoard(type, metadata, inits);
        this.author = _super.author;
        this.content = _super.content;
        this.createdAt = _super.createdAt;
        this.isDeleted = _super.isDeleted;
        this.title = _super.title;
        this.updatedAt = _super.updatedAt;
    }

}

