package com.dongsoop.dongsoop.recruitment.board.study.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QStudyBoardDepartment_StudyBoardDepartmentId is a Querydsl query type for StudyBoardDepartmentId
 */
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QStudyBoardDepartment_StudyBoardDepartmentId extends BeanPath<StudyBoardDepartment.StudyBoardDepartmentId> {

    private static final long serialVersionUID = 895521162L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QStudyBoardDepartment_StudyBoardDepartmentId studyBoardDepartmentId = new QStudyBoardDepartment_StudyBoardDepartmentId("studyBoardDepartmentId");

    public final com.dongsoop.dongsoop.department.entity.QDepartment department;

    public final QStudyBoard studyBoard;

    public QStudyBoardDepartment_StudyBoardDepartmentId(String variable) {
        this(StudyBoardDepartment.StudyBoardDepartmentId.class, forVariable(variable), INITS);
    }

    public QStudyBoardDepartment_StudyBoardDepartmentId(Path<? extends StudyBoardDepartment.StudyBoardDepartmentId> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QStudyBoardDepartment_StudyBoardDepartmentId(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QStudyBoardDepartment_StudyBoardDepartmentId(PathMetadata metadata, PathInits inits) {
        this(StudyBoardDepartment.StudyBoardDepartmentId.class, metadata, inits);
    }

    public QStudyBoardDepartment_StudyBoardDepartmentId(Class<? extends StudyBoardDepartment.StudyBoardDepartmentId> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.department = inits.isInitialized("department") ? new com.dongsoop.dongsoop.department.entity.QDepartment(forProperty("department")) : null;
        this.studyBoard = inits.isInitialized("studyBoard") ? new QStudyBoard(forProperty("studyBoard"), inits.get("studyBoard")) : null;
    }

}

