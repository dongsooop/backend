package com.dongsoop.dongsoop.member.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QMember is a Querydsl query type for Member
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMember extends EntityPathBase<Member> {

    private static final long serialVersionUID = 1923079806L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QMember member = new QMember("member1");

    public final com.dongsoop.dongsoop.common.QBaseEntity _super = new com.dongsoop.dongsoop.common.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final com.dongsoop.dongsoop.department.entity.QDepartment department;

    public final StringPath email = createString("email");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final BooleanPath isDeleted = _super.isDeleted;

    public final StringPath nickname = createString("nickname");

    public final StringPath password = createString("password");

    public final StringPath studentId = createString("studentId");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QMember(String variable) {
        this(Member.class, forVariable(variable), INITS);
    }

    public QMember(Path<? extends Member> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QMember(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QMember(PathMetadata metadata, PathInits inits) {
        this(Member.class, metadata, inits);
    }

    public QMember(Class<? extends Member> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.department = inits.isInitialized("department") ? new com.dongsoop.dongsoop.department.entity.QDepartment(forProperty("department")) : null;
    }

}

