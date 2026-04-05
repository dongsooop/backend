package com.dongsoop.dongsoop.notice.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QNotice_NoticeKey is a Querydsl query type for NoticeKey
 */
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QNotice_NoticeKey extends BeanPath<Notice.NoticeKey> {

    private static final long serialVersionUID = -476064301L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QNotice_NoticeKey noticeKey = new QNotice_NoticeKey("noticeKey");

    public final com.dongsoop.dongsoop.department.entity.QDepartment department;

    public final QNoticeDetails noticeDetails;

    public QNotice_NoticeKey(String variable) {
        this(Notice.NoticeKey.class, forVariable(variable), INITS);
    }

    public QNotice_NoticeKey(Path<? extends Notice.NoticeKey> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QNotice_NoticeKey(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QNotice_NoticeKey(PathMetadata metadata, PathInits inits) {
        this(Notice.NoticeKey.class, metadata, inits);
    }

    public QNotice_NoticeKey(Class<? extends Notice.NoticeKey> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.department = inits.isInitialized("department") ? new com.dongsoop.dongsoop.department.entity.QDepartment(forProperty("department")) : null;
        this.noticeDetails = inits.isInitialized("noticeDetails") ? new QNoticeDetails(forProperty("noticeDetails")) : null;
    }

}

