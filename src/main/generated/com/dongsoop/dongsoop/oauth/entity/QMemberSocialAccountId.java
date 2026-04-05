package com.dongsoop.dongsoop.oauth.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QMemberSocialAccountId is a Querydsl query type for MemberSocialAccountId
 */
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QMemberSocialAccountId extends BeanPath<MemberSocialAccountId> {

    private static final long serialVersionUID = 1193814586L;

    public static final QMemberSocialAccountId memberSocialAccountId = new QMemberSocialAccountId("memberSocialAccountId");

    public final StringPath providerId = createString("providerId");

    public final EnumPath<OAuthProviderType> providerType = createEnum("providerType", OAuthProviderType.class);

    public QMemberSocialAccountId(String variable) {
        super(MemberSocialAccountId.class, forVariable(variable));
    }

    public QMemberSocialAccountId(Path<? extends MemberSocialAccountId> path) {
        super(path.getType(), path.getMetadata());
    }

    public QMemberSocialAccountId(PathMetadata metadata) {
        super(MemberSocialAccountId.class, metadata);
    }

}

