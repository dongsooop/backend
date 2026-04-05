package com.dongsoop.dongsoop.role.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QMemberRole is a Querydsl query type for MemberRole
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMemberRole extends EntityPathBase<MemberRole> {

    private static final long serialVersionUID = -932806096L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QMemberRole memberRole = new QMemberRole("memberRole");

    public final QMemberRole_MemberRoleKey id;

    public QMemberRole(String variable) {
        this(MemberRole.class, forVariable(variable), INITS);
    }

    public QMemberRole(Path<? extends MemberRole> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QMemberRole(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QMemberRole(PathMetadata metadata, PathInits inits) {
        this(MemberRole.class, metadata, inits);
    }

    public QMemberRole(Class<? extends MemberRole> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.id = inits.isInitialized("id") ? new QMemberRole_MemberRoleKey(forProperty("id"), inits.get("id")) : null;
    }

}

