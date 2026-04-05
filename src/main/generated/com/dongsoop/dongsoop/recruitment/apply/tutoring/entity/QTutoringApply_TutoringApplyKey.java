package com.dongsoop.dongsoop.recruitment.apply.tutoring.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QTutoringApply_TutoringApplyKey is a Querydsl query type for TutoringApplyKey
 */
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QTutoringApply_TutoringApplyKey extends BeanPath<TutoringApply.TutoringApplyKey> {

    private static final long serialVersionUID = 1957037969L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QTutoringApply_TutoringApplyKey tutoringApplyKey = new QTutoringApply_TutoringApplyKey("tutoringApplyKey");

    public final com.dongsoop.dongsoop.member.entity.QMember member;

    public final com.dongsoop.dongsoop.recruitment.board.tutoring.entity.QTutoringBoard tutoringBoard;

    public QTutoringApply_TutoringApplyKey(String variable) {
        this(TutoringApply.TutoringApplyKey.class, forVariable(variable), INITS);
    }

    public QTutoringApply_TutoringApplyKey(Path<? extends TutoringApply.TutoringApplyKey> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QTutoringApply_TutoringApplyKey(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QTutoringApply_TutoringApplyKey(PathMetadata metadata, PathInits inits) {
        this(TutoringApply.TutoringApplyKey.class, metadata, inits);
    }

    public QTutoringApply_TutoringApplyKey(Class<? extends TutoringApply.TutoringApplyKey> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.member = inits.isInitialized("member") ? new com.dongsoop.dongsoop.member.entity.QMember(forProperty("member"), inits.get("member")) : null;
        this.tutoringBoard = inits.isInitialized("tutoringBoard") ? new com.dongsoop.dongsoop.recruitment.board.tutoring.entity.QTutoringBoard(forProperty("tutoringBoard"), inits.get("tutoringBoard")) : null;
    }

}

