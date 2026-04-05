package com.dongsoop.dongsoop.calendar.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QOfficialSchedule is a Querydsl query type for OfficialSchedule
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QOfficialSchedule extends EntityPathBase<OfficialSchedule> {

    private static final long serialVersionUID = -1206789398L;

    public static final QOfficialSchedule officialSchedule = new QOfficialSchedule("officialSchedule");

    public final com.dongsoop.dongsoop.common.QBaseEntity _super = new com.dongsoop.dongsoop.common.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final DatePath<java.time.LocalDate> endAt = createDate("endAt", java.time.LocalDate.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final BooleanPath isDeleted = _super.isDeleted;

    public final DatePath<java.time.LocalDate> startAt = createDate("startAt", java.time.LocalDate.class);

    public final StringPath title = createString("title");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QOfficialSchedule(String variable) {
        super(OfficialSchedule.class, forVariable(variable));
    }

    public QOfficialSchedule(Path<? extends OfficialSchedule> path) {
        super(path.getType(), path.getMetadata());
    }

    public QOfficialSchedule(PathMetadata metadata) {
        super(OfficialSchedule.class, metadata);
    }

}

