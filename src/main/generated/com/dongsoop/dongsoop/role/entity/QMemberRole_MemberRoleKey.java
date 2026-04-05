package com.dongsoop.dongsoop.role.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QMemberRole_MemberRoleKey is a Querydsl query type for MemberRoleKey
 */
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QMemberRole_MemberRoleKey extends BeanPath<MemberRole.MemberRoleKey> {

    private static final long serialVersionUID = 526741169L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QMemberRole_MemberRoleKey memberRoleKey = new QMemberRole_MemberRoleKey("memberRoleKey");

    public final com.dongsoop.dongsoop.member.entity.QMember member;

    public final QRole role;

    public QMemberRole_MemberRoleKey(String variable) {
        this(MemberRole.MemberRoleKey.class, forVariable(variable), INITS);
    }

    public QMemberRole_MemberRoleKey(Path<? extends MemberRole.MemberRoleKey> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QMemberRole_MemberRoleKey(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QMemberRole_MemberRoleKey(PathMetadata metadata, PathInits inits) {
        this(MemberRole.MemberRoleKey.class, metadata, inits);
    }

    public QMemberRole_MemberRoleKey(Class<? extends MemberRole.MemberRoleKey> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.member = inits.isInitialized("member") ? new com.dongsoop.dongsoop.member.entity.QMember(forProperty("member"), inits.get("member")) : null;
        this.role = inits.isInitialized("role") ? new QRole(forProperty("role")) : null;
    }

}

