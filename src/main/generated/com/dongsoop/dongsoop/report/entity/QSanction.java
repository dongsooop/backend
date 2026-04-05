package com.dongsoop.dongsoop.report.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QSanction is a Querydsl query type for Sanction
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QSanction extends EntityPathBase<Sanction> {

    private static final long serialVersionUID = 1300192149L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QSanction sanction = new QSanction("sanction");

    public final com.dongsoop.dongsoop.common.QBaseEntity _super = new com.dongsoop.dongsoop.common.QBaseEntity(this);

    public final com.dongsoop.dongsoop.member.entity.QMember admin;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final StringPath description = createString("description");

    public final DateTimePath<java.time.LocalDateTime> endDate = createDateTime("endDate", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final BooleanPath isActive = createBoolean("isActive");

    //inherited
    public final BooleanPath isDeleted = _super.isDeleted;

    public final com.dongsoop.dongsoop.member.entity.QMember member;

    public final StringPath reason = createString("reason");

    public final QReport report;

    public final EnumPath<SanctionType> sanctionType = createEnum("sanctionType", SanctionType.class);

    public final DateTimePath<java.time.LocalDateTime> startDate = createDateTime("startDate", java.time.LocalDateTime.class);

    public final com.dongsoop.dongsoop.member.entity.QMember targetMember;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QSanction(String variable) {
        this(Sanction.class, forVariable(variable), INITS);
    }

    public QSanction(Path<? extends Sanction> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QSanction(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QSanction(PathMetadata metadata, PathInits inits) {
        this(Sanction.class, metadata, inits);
    }

    public QSanction(Class<? extends Sanction> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.admin = inits.isInitialized("admin") ? new com.dongsoop.dongsoop.member.entity.QMember(forProperty("admin"), inits.get("admin")) : null;
        this.member = inits.isInitialized("member") ? new com.dongsoop.dongsoop.member.entity.QMember(forProperty("member"), inits.get("member")) : null;
        this.report = inits.isInitialized("report") ? new QReport(forProperty("report"), inits.get("report")) : null;
        this.targetMember = inits.isInitialized("targetMember") ? new com.dongsoop.dongsoop.member.entity.QMember(forProperty("targetMember"), inits.get("targetMember")) : null;
    }

}

