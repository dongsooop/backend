package com.dongsoop.dongsoop.recruitment.board.study.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QStudyBoardDepartment is a Querydsl query type for StudyBoardDepartment
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QStudyBoardDepartment extends EntityPathBase<StudyBoardDepartment> {

    private static final long serialVersionUID = -1227721778L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QStudyBoardDepartment studyBoardDepartment = new QStudyBoardDepartment("studyBoardDepartment");

    public final QStudyBoardDepartment_StudyBoardDepartmentId id;

    public QStudyBoardDepartment(String variable) {
        this(StudyBoardDepartment.class, forVariable(variable), INITS);
    }

    public QStudyBoardDepartment(Path<? extends StudyBoardDepartment> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QStudyBoardDepartment(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QStudyBoardDepartment(PathMetadata metadata, PathInits inits) {
        this(StudyBoardDepartment.class, metadata, inits);
    }

    public QStudyBoardDepartment(Class<? extends StudyBoardDepartment> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.id = inits.isInitialized("id") ? new QStudyBoardDepartment_StudyBoardDepartmentId(forProperty("id"), inits.get("id")) : null;
    }

}

