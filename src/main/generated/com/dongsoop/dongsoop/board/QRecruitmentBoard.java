package com.dongsoop.dongsoop.board;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QRecruitmentBoard is a Querydsl query type for RecruitmentBoard
 */
@Generated("com.querydsl.codegen.DefaultSupertypeSerializer")
public class QRecruitmentBoard extends EntityPathBase<RecruitmentBoard> {

    private static final long serialVersionUID = 1174489407L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QRecruitmentBoard recruitmentBoard = new QRecruitmentBoard("recruitmentBoard");

    public final QBoard _super;

    // inherited
    public final com.dongsoop.dongsoop.member.entity.QMember author;

    //inherited
    public final StringPath content;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt;

    public final DateTimePath<java.time.LocalDateTime> endAt = createDateTime("endAt", java.time.LocalDateTime.class);

    //inherited
    public final BooleanPath isDeleted;

    public final StringPath RoomId = createString("RoomId");

    public final DateTimePath<java.time.LocalDateTime> startAt = createDateTime("startAt", java.time.LocalDateTime.class);

    public final StringPath tags = createString("tags");

    //inherited
    public final StringPath title;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt;

    public QRecruitmentBoard(String variable) {
        this(RecruitmentBoard.class, forVariable(variable), INITS);
    }

    public QRecruitmentBoard(Path<? extends RecruitmentBoard> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QRecruitmentBoard(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QRecruitmentBoard(PathMetadata metadata, PathInits inits) {
        this(RecruitmentBoard.class, metadata, inits);
    }

    public QRecruitmentBoard(Class<? extends RecruitmentBoard> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this._super = new QBoard(type, metadata, inits);
        this.author = _super.author;
        this.content = _super.content;
        this.createdAt = _super.createdAt;
        this.isDeleted = _super.isDeleted;
        this.title = _super.title;
        this.updatedAt = _super.updatedAt;
    }

}

