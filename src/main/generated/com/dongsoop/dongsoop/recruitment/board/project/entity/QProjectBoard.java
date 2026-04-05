package com.dongsoop.dongsoop.recruitment.board.project.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QProjectBoard is a Querydsl query type for ProjectBoard
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QProjectBoard extends EntityPathBase<ProjectBoard> {

    private static final long serialVersionUID = -314573988L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QProjectBoard projectBoard = new QProjectBoard("projectBoard");

    public final com.dongsoop.dongsoop.board.QRecruitmentBoard _super;

    // inherited
    public final com.dongsoop.dongsoop.member.entity.QMember author;

    //inherited
    public final StringPath content;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> endAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final BooleanPath isDeleted;

    //inherited
    public final StringPath RoomId;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> startAt;

    //inherited
    public final StringPath tags;

    //inherited
    public final StringPath title;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt;

    public QProjectBoard(String variable) {
        this(ProjectBoard.class, forVariable(variable), INITS);
    }

    public QProjectBoard(Path<? extends ProjectBoard> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QProjectBoard(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QProjectBoard(PathMetadata metadata, PathInits inits) {
        this(ProjectBoard.class, metadata, inits);
    }

    public QProjectBoard(Class<? extends ProjectBoard> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this._super = new com.dongsoop.dongsoop.board.QRecruitmentBoard(type, metadata, inits);
        this.author = _super.author;
        this.content = _super.content;
        this.createdAt = _super.createdAt;
        this.endAt = _super.endAt;
        this.isDeleted = _super.isDeleted;
        this.RoomId = _super.RoomId;
        this.startAt = _super.startAt;
        this.tags = _super.tags;
        this.title = _super.title;
        this.updatedAt = _super.updatedAt;
    }

}

