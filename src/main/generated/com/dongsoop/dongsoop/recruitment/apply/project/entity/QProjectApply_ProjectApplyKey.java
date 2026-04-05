package com.dongsoop.dongsoop.recruitment.apply.project.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QProjectApply_ProjectApplyKey is a Querydsl query type for ProjectApplyKey
 */
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QProjectApply_ProjectApplyKey extends BeanPath<ProjectApply.ProjectApplyKey> {

    private static final long serialVersionUID = -234483544L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QProjectApply_ProjectApplyKey projectApplyKey = new QProjectApply_ProjectApplyKey("projectApplyKey");

    public final com.dongsoop.dongsoop.member.entity.QMember member;

    public final com.dongsoop.dongsoop.recruitment.board.project.entity.QProjectBoard projectBoard;

    public QProjectApply_ProjectApplyKey(String variable) {
        this(ProjectApply.ProjectApplyKey.class, forVariable(variable), INITS);
    }

    public QProjectApply_ProjectApplyKey(Path<? extends ProjectApply.ProjectApplyKey> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QProjectApply_ProjectApplyKey(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QProjectApply_ProjectApplyKey(PathMetadata metadata, PathInits inits) {
        this(ProjectApply.ProjectApplyKey.class, metadata, inits);
    }

    public QProjectApply_ProjectApplyKey(Class<? extends ProjectApply.ProjectApplyKey> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.member = inits.isInitialized("member") ? new com.dongsoop.dongsoop.member.entity.QMember(forProperty("member"), inits.get("member")) : null;
        this.projectBoard = inits.isInitialized("projectBoard") ? new com.dongsoop.dongsoop.recruitment.board.project.entity.QProjectBoard(forProperty("projectBoard"), inits.get("projectBoard")) : null;
    }

}

