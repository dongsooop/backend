package com.dongsoop.dongsoop.board;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QBoard is a Querydsl query type for Board
 */
@Generated("com.querydsl.codegen.DefaultSupertypeSerializer")
public class QBoard extends EntityPathBase<Board> {

    private static final long serialVersionUID = 2055750513L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QBoard board = new QBoard("board");

    public final com.dongsoop.dongsoop.common.QBaseEntity _super = new com.dongsoop.dongsoop.common.QBaseEntity(this);

    public final com.dongsoop.dongsoop.member.entity.QMember author;

    public final StringPath content = createString("content");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final BooleanPath isDeleted = _super.isDeleted;

    public final StringPath title = createString("title");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QBoard(String variable) {
        this(Board.class, forVariable(variable), INITS);
    }

    public QBoard(Path<? extends Board> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QBoard(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QBoard(PathMetadata metadata, PathInits inits) {
        this(Board.class, metadata, inits);
    }

    public QBoard(Class<? extends Board> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.author = inits.isInitialized("author") ? new com.dongsoop.dongsoop.member.entity.QMember(forProperty("author"), inits.get("author")) : null;
    }

}

