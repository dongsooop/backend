package com.dongsoop.dongsoop.notification.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QMemberNotificationId is a Querydsl query type for MemberNotificationId
 */
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QMemberNotificationId extends BeanPath<MemberNotificationId> {

    private static final long serialVersionUID = -1873961323L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QMemberNotificationId memberNotificationId = new QMemberNotificationId("memberNotificationId");

    public final QNotificationDetails details;

    public final com.dongsoop.dongsoop.member.entity.QMember member;

    public QMemberNotificationId(String variable) {
        this(MemberNotificationId.class, forVariable(variable), INITS);
    }

    public QMemberNotificationId(Path<? extends MemberNotificationId> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QMemberNotificationId(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QMemberNotificationId(PathMetadata metadata, PathInits inits) {
        this(MemberNotificationId.class, metadata, inits);
    }

    public QMemberNotificationId(Class<? extends MemberNotificationId> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.details = inits.isInitialized("details") ? new QNotificationDetails(forProperty("details")) : null;
        this.member = inits.isInitialized("member") ? new com.dongsoop.dongsoop.member.entity.QMember(forProperty("member"), inits.get("member")) : null;
    }

}

