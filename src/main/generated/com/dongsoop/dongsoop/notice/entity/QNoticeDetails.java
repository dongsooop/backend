package com.dongsoop.dongsoop.notice.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QNoticeDetails is a Querydsl query type for NoticeDetails
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QNoticeDetails extends EntityPathBase<NoticeDetails> {

    private static final long serialVersionUID = 1124858472L;

    public static final QNoticeDetails noticeDetails = new QNoticeDetails("noticeDetails");

    public final DatePath<java.time.LocalDate> createdAt = createDate("createdAt", java.time.LocalDate.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath link = createString("link");

    public final StringPath title = createString("title");

    public final StringPath writer = createString("writer");

    public QNoticeDetails(String variable) {
        super(NoticeDetails.class, forVariable(variable));
    }

    public QNoticeDetails(Path<? extends NoticeDetails> path) {
        super(path.getType(), path.getMetadata());
    }

    public QNoticeDetails(PathMetadata metadata) {
        super(NoticeDetails.class, metadata);
    }

}

