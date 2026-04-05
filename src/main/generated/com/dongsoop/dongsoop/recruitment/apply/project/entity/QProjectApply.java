package com.dongsoop.dongsoop.recruitment.apply.project.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QProjectApply is a Querydsl query type for ProjectApply
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QProjectApply extends EntityPathBase<ProjectApply> {

    private static final long serialVersionUID = -1867478932L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QProjectApply projectApply = new QProjectApply("projectApply");

    public final DateTimePath<java.time.LocalDateTime> applyTime = createDateTime("applyTime", java.time.LocalDateTime.class);

    public final QProjectApply_ProjectApplyKey id;

    public final StringPath introduction = createString("introduction");

    public final StringPath motivation = createString("motivation");

    public final EnumPath<com.dongsoop.dongsoop.recruitment.apply.entity.RecruitmentApplyStatus> status = createEnum("status", com.dongsoop.dongsoop.recruitment.apply.entity.RecruitmentApplyStatus.class);

    public QProjectApply(String variable) {
        this(ProjectApply.class, forVariable(variable), INITS);
    }

    public QProjectApply(Path<? extends ProjectApply> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QProjectApply(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QProjectApply(PathMetadata metadata, PathInits inits) {
        this(ProjectApply.class, metadata, inits);
    }

    public QProjectApply(Class<? extends ProjectApply> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.id = inits.isInitialized("id") ? new QProjectApply_ProjectApplyKey(forProperty("id"), inits.get("id")) : null;
    }

}

