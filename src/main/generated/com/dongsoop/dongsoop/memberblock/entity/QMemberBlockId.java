package com.dongsoop.dongsoop.memberblock.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QMemberBlockId is a Querydsl query type for MemberBlockId
 */
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QMemberBlockId extends BeanPath<MemberBlockId> {

    private static final long serialVersionUID = 40018091L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QMemberBlockId memberBlockId = new QMemberBlockId("memberBlockId");

    public final com.dongsoop.dongsoop.member.entity.QMember blockedMember;

    public final com.dongsoop.dongsoop.member.entity.QMember blocker;

    public QMemberBlockId(String variable) {
        this(MemberBlockId.class, forVariable(variable), INITS);
    }

    public QMemberBlockId(Path<? extends MemberBlockId> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QMemberBlockId(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QMemberBlockId(PathMetadata metadata, PathInits inits) {
        this(MemberBlockId.class, metadata, inits);
    }

    public QMemberBlockId(Class<? extends MemberBlockId> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.blockedMember = inits.isInitialized("blockedMember") ? new com.dongsoop.dongsoop.member.entity.QMember(forProperty("blockedMember"), inits.get("blockedMember")) : null;
        this.blocker = inits.isInitialized("blocker") ? new com.dongsoop.dongsoop.member.entity.QMember(forProperty("blocker"), inits.get("blocker")) : null;
    }

}

