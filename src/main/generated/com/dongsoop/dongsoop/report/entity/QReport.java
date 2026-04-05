package com.dongsoop.dongsoop.report.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QReport is a Querydsl query type for Report
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QReport extends EntityPathBase<Report> {

    private static final long serialVersionUID = -1882726862L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QReport report = new QReport("report");

    public final com.dongsoop.dongsoop.common.QBaseEntity _super = new com.dongsoop.dongsoop.common.QBaseEntity(this);

    public final com.dongsoop.dongsoop.member.entity.QMember admin;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final StringPath description = createString("description");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final BooleanPath isDeleted = _super.isDeleted;

    public final BooleanPath isProcessed = createBoolean("isProcessed");

    public final BooleanPath isSanctionActive = createBoolean("isSanctionActive");

    public final com.dongsoop.dongsoop.member.entity.QMember reporter;

    public final EnumPath<ReportReason> reportReason = createEnum("reportReason", ReportReason.class);

    public final EnumPath<ReportType> reportType = createEnum("reportType", ReportType.class);

    public final QSanction sanction;

    public final NumberPath<Long> targetId = createNumber("targetId", Long.class);

    public final com.dongsoop.dongsoop.member.entity.QMember targetMember;

    public final StringPath targetUrl = createString("targetUrl");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QReport(String variable) {
        this(Report.class, forVariable(variable), INITS);
    }

    public QReport(Path<? extends Report> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QReport(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QReport(PathMetadata metadata, PathInits inits) {
        this(Report.class, metadata, inits);
    }

    public QReport(Class<? extends Report> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.admin = inits.isInitialized("admin") ? new com.dongsoop.dongsoop.member.entity.QMember(forProperty("admin"), inits.get("admin")) : null;
        this.reporter = inits.isInitialized("reporter") ? new com.dongsoop.dongsoop.member.entity.QMember(forProperty("reporter"), inits.get("reporter")) : null;
        this.sanction = inits.isInitialized("sanction") ? new QSanction(forProperty("sanction"), inits.get("sanction")) : null;
        this.targetMember = inits.isInitialized("targetMember") ? new com.dongsoop.dongsoop.member.entity.QMember(forProperty("targetMember"), inits.get("targetMember")) : null;
    }

}

