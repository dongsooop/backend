package com.dongsoop.dongsoop.recruitment.apply.tutoring.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QTutoringApply is a Querydsl query type for TutoringApply
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QTutoringApply extends EntityPathBase<TutoringApply> {

    private static final long serialVersionUID = -516824806L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QTutoringApply tutoringApply = new QTutoringApply("tutoringApply");

    public final com.dongsoop.dongsoop.common.QBaseEntity _super = new com.dongsoop.dongsoop.common.QBaseEntity(this);

    public final DateTimePath<java.time.LocalDateTime> applyTime = createDateTime("applyTime", java.time.LocalDateTime.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final QTutoringApply_TutoringApplyKey id;

    public final StringPath introduction = createString("introduction");

    //inherited
    public final BooleanPath isDeleted = _super.isDeleted;

    public final StringPath motivation = createString("motivation");

    public final EnumPath<com.dongsoop.dongsoop.recruitment.apply.entity.RecruitmentApplyStatus> status = createEnum("status", com.dongsoop.dongsoop.recruitment.apply.entity.RecruitmentApplyStatus.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QTutoringApply(String variable) {
        this(TutoringApply.class, forVariable(variable), INITS);
    }

    public QTutoringApply(Path<? extends TutoringApply> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QTutoringApply(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QTutoringApply(PathMetadata metadata, PathInits inits) {
        this(TutoringApply.class, metadata, inits);
    }

    public QTutoringApply(Class<? extends TutoringApply> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.id = inits.isInitialized("id") ? new QTutoringApply_TutoringApplyKey(forProperty("id"), inits.get("id")) : null;
    }

}

