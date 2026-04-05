package com.dongsoop.dongsoop.calendar.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QMemberSchedule is a Querydsl query type for MemberSchedule
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMemberSchedule extends EntityPathBase<MemberSchedule> {

    private static final long serialVersionUID = -1774772839L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QMemberSchedule memberSchedule = new QMemberSchedule("memberSchedule");

    public final com.dongsoop.dongsoop.common.QBaseEntity _super = new com.dongsoop.dongsoop.common.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final DateTimePath<java.time.LocalDateTime> endAt = createDateTime("endAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final BooleanPath isDeleted = _super.isDeleted;

    public final StringPath location = createString("location");

    public final com.dongsoop.dongsoop.member.entity.QMember member;

    public final DateTimePath<java.time.LocalDateTime> startAt = createDateTime("startAt", java.time.LocalDateTime.class);

    public final StringPath title = createString("title");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QMemberSchedule(String variable) {
        this(MemberSchedule.class, forVariable(variable), INITS);
    }

    public QMemberSchedule(Path<? extends MemberSchedule> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QMemberSchedule(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QMemberSchedule(PathMetadata metadata, PathInits inits) {
        this(MemberSchedule.class, metadata, inits);
    }

    public QMemberSchedule(Class<? extends MemberSchedule> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.member = inits.isInitialized("member") ? new com.dongsoop.dongsoop.member.entity.QMember(forProperty("member"), inits.get("member")) : null;
    }

}

