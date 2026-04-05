package com.dongsoop.dongsoop.memberdevice.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QMemberDevice is a Querydsl query type for MemberDevice
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMemberDevice extends EntityPathBase<MemberDevice> {

    private static final long serialVersionUID = 30877834L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QMemberDevice memberDevice = new QMemberDevice("memberDevice");

    public final com.dongsoop.dongsoop.common.QBaseEntity _super = new com.dongsoop.dongsoop.common.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final StringPath deviceToken = createString("deviceToken");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final BooleanPath isDeleted = _super.isDeleted;

    public final com.dongsoop.dongsoop.member.entity.QMember member;

    public final EnumPath<MemberDeviceType> memberDeviceType = createEnum("memberDeviceType", MemberDeviceType.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QMemberDevice(String variable) {
        this(MemberDevice.class, forVariable(variable), INITS);
    }

    public QMemberDevice(Path<? extends MemberDevice> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QMemberDevice(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QMemberDevice(PathMetadata metadata, PathInits inits) {
        this(MemberDevice.class, metadata, inits);
    }

    public QMemberDevice(Class<? extends MemberDevice> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.member = inits.isInitialized("member") ? new com.dongsoop.dongsoop.member.entity.QMember(forProperty("member"), inits.get("member")) : null;
    }

}

