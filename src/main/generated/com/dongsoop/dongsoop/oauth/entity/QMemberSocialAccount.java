package com.dongsoop.dongsoop.oauth.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QMemberSocialAccount is a Querydsl query type for MemberSocialAccount
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMemberSocialAccount extends EntityPathBase<MemberSocialAccount> {

    private static final long serialVersionUID = -1200991041L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QMemberSocialAccount memberSocialAccount = new QMemberSocialAccount("memberSocialAccount");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final QMemberSocialAccountId id;

    public final com.dongsoop.dongsoop.member.entity.QMember member;

    public QMemberSocialAccount(String variable) {
        this(MemberSocialAccount.class, forVariable(variable), INITS);
    }

    public QMemberSocialAccount(Path<? extends MemberSocialAccount> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QMemberSocialAccount(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QMemberSocialAccount(PathMetadata metadata, PathInits inits) {
        this(MemberSocialAccount.class, metadata, inits);
    }

    public QMemberSocialAccount(Class<? extends MemberSocialAccount> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.id = inits.isInitialized("id") ? new QMemberSocialAccountId(forProperty("id")) : null;
        this.member = inits.isInitialized("member") ? new com.dongsoop.dongsoop.member.entity.QMember(forProperty("member"), inits.get("member")) : null;
    }

}

