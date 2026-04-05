package com.dongsoop.dongsoop.recruitment.apply.study.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QStudyApply_StudyApplyKey is a Querydsl query type for StudyApplyKey
 */
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QStudyApply_StudyApplyKey extends BeanPath<StudyApply.StudyApplyKey> {

    private static final long serialVersionUID = -2145287464L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QStudyApply_StudyApplyKey studyApplyKey = new QStudyApply_StudyApplyKey("studyApplyKey");

    public final com.dongsoop.dongsoop.member.entity.QMember member;

    public final com.dongsoop.dongsoop.recruitment.board.study.entity.QStudyBoard studyBoard;

    public QStudyApply_StudyApplyKey(String variable) {
        this(StudyApply.StudyApplyKey.class, forVariable(variable), INITS);
    }

    public QStudyApply_StudyApplyKey(Path<? extends StudyApply.StudyApplyKey> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QStudyApply_StudyApplyKey(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QStudyApply_StudyApplyKey(PathMetadata metadata, PathInits inits) {
        this(StudyApply.StudyApplyKey.class, metadata, inits);
    }

    public QStudyApply_StudyApplyKey(Class<? extends StudyApply.StudyApplyKey> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.member = inits.isInitialized("member") ? new com.dongsoop.dongsoop.member.entity.QMember(forProperty("member"), inits.get("member")) : null;
        this.studyBoard = inits.isInitialized("studyBoard") ? new com.dongsoop.dongsoop.recruitment.board.study.entity.QStudyBoard(forProperty("studyBoard"), inits.get("studyBoard")) : null;
    }

}

