package com.dongsoop.dongsoop.recruitment.apply.study.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QStudyApply is a Querydsl query type for StudyApply
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QStudyApply extends EntityPathBase<StudyApply> {

    private static final long serialVersionUID = 196405356L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QStudyApply studyApply = new QStudyApply("studyApply");

    public final DateTimePath<java.time.LocalDateTime> applyTime = createDateTime("applyTime", java.time.LocalDateTime.class);

    public final QStudyApply_StudyApplyKey id;

    public final StringPath introduction = createString("introduction");

    public final StringPath motivation = createString("motivation");

    public final EnumPath<com.dongsoop.dongsoop.recruitment.apply.entity.RecruitmentApplyStatus> status = createEnum("status", com.dongsoop.dongsoop.recruitment.apply.entity.RecruitmentApplyStatus.class);

    public QStudyApply(String variable) {
        this(StudyApply.class, forVariable(variable), INITS);
    }

    public QStudyApply(Path<? extends StudyApply> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QStudyApply(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QStudyApply(PathMetadata metadata, PathInits inits) {
        this(StudyApply.class, metadata, inits);
    }

    public QStudyApply(Class<? extends StudyApply> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.id = inits.isInitialized("id") ? new QStudyApply_StudyApplyKey(forProperty("id"), inits.get("id")) : null;
    }

}

