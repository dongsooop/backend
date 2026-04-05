package com.dongsoop.dongsoop.recruitment.board.project.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QProjectBoardDepartment is a Querydsl query type for ProjectBoardDepartment
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QProjectBoardDepartment extends EntityPathBase<ProjectBoardDepartment> {

    private static final long serialVersionUID = 290625486L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QProjectBoardDepartment projectBoardDepartment = new QProjectBoardDepartment("projectBoardDepartment");

    public final QProjectBoardDepartment_ProjectBoardDepartmentId id;

    public QProjectBoardDepartment(String variable) {
        this(ProjectBoardDepartment.class, forVariable(variable), INITS);
    }

    public QProjectBoardDepartment(Path<? extends ProjectBoardDepartment> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QProjectBoardDepartment(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QProjectBoardDepartment(PathMetadata metadata, PathInits inits) {
        this(ProjectBoardDepartment.class, metadata, inits);
    }

    public QProjectBoardDepartment(Class<? extends ProjectBoardDepartment> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.id = inits.isInitialized("id") ? new QProjectBoardDepartment_ProjectBoardDepartmentId(forProperty("id"), inits.get("id")) : null;
    }

}

