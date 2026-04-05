package com.dongsoop.dongsoop.feedback.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QFeedback is a Querydsl query type for Feedback
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QFeedback extends EntityPathBase<Feedback> {

    private static final long serialVersionUID = 1804376596L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QFeedback feedback = new QFeedback("feedback");

    public final com.dongsoop.dongsoop.common.QBaseEntity _super = new com.dongsoop.dongsoop.common.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final StringPath featureRequests = createString("featureRequests");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath improvementSuggestions = createString("improvementSuggestions");

    //inherited
    public final BooleanPath isDeleted = _super.isDeleted;

    public final com.dongsoop.dongsoop.member.entity.QMember member;

    public final EnumPath<FeedbackStatus> status = createEnum("status", FeedbackStatus.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QFeedback(String variable) {
        this(Feedback.class, forVariable(variable), INITS);
    }

    public QFeedback(Path<? extends Feedback> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QFeedback(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QFeedback(PathMetadata metadata, PathInits inits) {
        this(Feedback.class, metadata, inits);
    }

    public QFeedback(Class<? extends Feedback> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.member = inits.isInitialized("member") ? new com.dongsoop.dongsoop.member.entity.QMember(forProperty("member"), inits.get("member")) : null;
    }

}

