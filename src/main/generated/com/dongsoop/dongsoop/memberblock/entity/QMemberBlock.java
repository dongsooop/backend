package com.dongsoop.dongsoop.memberblock.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QMemberBlock is a Querydsl query type for MemberBlock
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMemberBlock extends EntityPathBase<MemberBlock> {

    private static final long serialVersionUID = -66997392L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QMemberBlock memberBlock = new QMemberBlock("memberBlock");

    public final QMemberBlockId id;

    public QMemberBlock(String variable) {
        this(MemberBlock.class, forVariable(variable), INITS);
    }

    public QMemberBlock(Path<? extends MemberBlock> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QMemberBlock(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QMemberBlock(PathMetadata metadata, PathInits inits) {
        this(MemberBlock.class, metadata, inits);
    }

    public QMemberBlock(Class<? extends MemberBlock> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.id = inits.isInitialized("id") ? new QMemberBlockId(forProperty("id"), inits.get("id")) : null;
    }

}

