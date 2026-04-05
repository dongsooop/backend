package com.dongsoop.dongsoop.notification.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QMemberNotification is a Querydsl query type for MemberNotification
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMemberNotification extends EntityPathBase<MemberNotification> {

    private static final long serialVersionUID = -712563750L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QMemberNotification memberNotification = new QMemberNotification("memberNotification");

    public final com.dongsoop.dongsoop.common.QBaseEntity _super = new com.dongsoop.dongsoop.common.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final QMemberNotificationId id;

    //inherited
    public final BooleanPath isDeleted = _super.isDeleted;

    public final BooleanPath isRead = createBoolean("isRead");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QMemberNotification(String variable) {
        this(MemberNotification.class, forVariable(variable), INITS);
    }

    public QMemberNotification(Path<? extends MemberNotification> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QMemberNotification(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QMemberNotification(PathMetadata metadata, PathInits inits) {
        this(MemberNotification.class, metadata, inits);
    }

    public QMemberNotification(Class<? extends MemberNotification> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.id = inits.isInitialized("id") ? new QMemberNotificationId(forProperty("id"), inits.get("id")) : null;
    }

}

