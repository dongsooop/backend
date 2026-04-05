package com.dongsoop.dongsoop.recruitment.board.project.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QProjectBoardDepartment_ProjectBoardDepartmentId is a Querydsl query type for ProjectBoardDepartmentId
 */
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QProjectBoardDepartment_ProjectBoardDepartmentId extends BeanPath<ProjectBoardDepartment.ProjectBoardDepartmentId> {

    private static final long serialVersionUID = -1472828870L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QProjectBoardDepartment_ProjectBoardDepartmentId projectBoardDepartmentId = new QProjectBoardDepartment_ProjectBoardDepartmentId("projectBoardDepartmentId");

    public final com.dongsoop.dongsoop.department.entity.QDepartment department;

    public final QProjectBoard projectBoard;

    public QProjectBoardDepartment_ProjectBoardDepartmentId(String variable) {
        this(ProjectBoardDepartment.ProjectBoardDepartmentId.class, forVariable(variable), INITS);
    }

    public QProjectBoardDepartment_ProjectBoardDepartmentId(Path<? extends ProjectBoardDepartment.ProjectBoardDepartmentId> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QProjectBoardDepartment_ProjectBoardDepartmentId(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QProjectBoardDepartment_ProjectBoardDepartmentId(PathMetadata metadata, PathInits inits) {
        this(ProjectBoardDepartment.ProjectBoardDepartmentId.class, metadata, inits);
    }

    public QProjectBoardDepartment_ProjectBoardDepartmentId(Class<? extends ProjectBoardDepartment.ProjectBoardDepartmentId> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.department = inits.isInitialized("department") ? new com.dongsoop.dongsoop.department.entity.QDepartment(forProperty("department")) : null;
        this.projectBoard = inits.isInitialized("projectBoard") ? new QProjectBoard(forProperty("projectBoard"), inits.get("projectBoard")) : null;
    }

}

