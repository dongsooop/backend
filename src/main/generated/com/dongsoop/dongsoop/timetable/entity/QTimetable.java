package com.dongsoop.dongsoop.timetable.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QTimetable is a Querydsl query type for Timetable
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QTimetable extends EntityPathBase<Timetable> {

    private static final long serialVersionUID = -126216240L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QTimetable timetable = new QTimetable("timetable");

    public final com.dongsoop.dongsoop.common.QBaseEntity _super = new com.dongsoop.dongsoop.common.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final TimePath<java.time.LocalTime> endAt = createTime("endAt", java.time.LocalTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final BooleanPath isDeleted = _super.isDeleted;

    public final StringPath location = createString("location");

    public final com.dongsoop.dongsoop.member.entity.QMember member;

    public final StringPath name = createString("name");

    public final StringPath professor = createString("professor");

    public final EnumPath<SemesterType> semester = createEnum("semester", SemesterType.class);

    public final TimePath<java.time.LocalTime> startAt = createTime("startAt", java.time.LocalTime.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final EnumPath<java.time.DayOfWeek> week = createEnum("week", java.time.DayOfWeek.class);

    public final ComparablePath<java.time.Year> year = createComparable("year", java.time.Year.class);

    public QTimetable(String variable) {
        this(Timetable.class, forVariable(variable), INITS);
    }

    public QTimetable(Path<? extends Timetable> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QTimetable(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QTimetable(PathMetadata metadata, PathInits inits) {
        this(Timetable.class, metadata, inits);
    }

    public QTimetable(Class<? extends Timetable> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.member = inits.isInitialized("member") ? new com.dongsoop.dongsoop.member.entity.QMember(forProperty("member"), inits.get("member")) : null;
    }

}

