package com.dongsoop.dongsoop.recruitment.board.tutoring.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QTutoringBoard is a Querydsl query type for TutoringBoard
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QTutoringBoard extends EntityPathBase<TutoringBoard> {

    private static final long serialVersionUID = 626873866L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QTutoringBoard tutoringBoard = new QTutoringBoard("tutoringBoard");

    public final com.dongsoop.dongsoop.board.QRecruitmentBoard _super;

    // inherited
    public final com.dongsoop.dongsoop.member.entity.QMember author;

    //inherited
    public final StringPath content;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt;

    public final com.dongsoop.dongsoop.department.entity.QDepartment department;

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

    public QTutoringBoard(String variable) {
        this(TutoringBoard.class, forVariable(variable), INITS);
    }

    public QTutoringBoard(Path<? extends TutoringBoard> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QTutoringBoard(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QTutoringBoard(PathMetadata metadata, PathInits inits) {
        this(TutoringBoard.class, metadata, inits);
    }

    public QTutoringBoard(Class<? extends TutoringBoard> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this._super = new com.dongsoop.dongsoop.board.QRecruitmentBoard(type, metadata, inits);
        this.author = _super.author;
        this.content = _super.content;
        this.createdAt = _super.createdAt;
        this.department = inits.isInitialized("department") ? new com.dongsoop.dongsoop.department.entity.QDepartment(forProperty("department")) : null;
        this.endAt = _super.endAt;
        this.isDeleted = _super.isDeleted;
        this.RoomId = _super.RoomId;
        this.startAt = _super.startAt;
        this.tags = _super.tags;
        this.title = _super.title;
        this.updatedAt = _super.updatedAt;
    }

}

