package com.dongsoop.dongsoop.recruitment.board.study.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QStudyBoard is a Querydsl query type for StudyBoard
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QStudyBoard extends EntityPathBase<StudyBoard> {

    private static final long serialVersionUID = 463656796L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QStudyBoard studyBoard = new QStudyBoard("studyBoard");

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

    public QStudyBoard(String variable) {
        this(StudyBoard.class, forVariable(variable), INITS);
    }

    public QStudyBoard(Path<? extends StudyBoard> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QStudyBoard(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QStudyBoard(PathMetadata metadata, PathInits inits) {
        this(StudyBoard.class, metadata, inits);
    }

    public QStudyBoard(Class<? extends StudyBoard> type, PathMetadata metadata, PathInits inits) {
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

