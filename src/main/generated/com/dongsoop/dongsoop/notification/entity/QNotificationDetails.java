package com.dongsoop.dongsoop.notification.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QNotificationDetails is a Querydsl query type for NotificationDetails
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QNotificationDetails extends EntityPathBase<NotificationDetails> {

    private static final long serialVersionUID = 556844610L;

    public static final QNotificationDetails notificationDetails = new QNotificationDetails("notificationDetails");

    public final com.dongsoop.dongsoop.common.QBaseEntity _super = new com.dongsoop.dongsoop.common.QBaseEntity(this);

    public final StringPath body = createString("body");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final BooleanPath isDeleted = _super.isDeleted;

    public final StringPath title = createString("title");

    public final EnumPath<com.dongsoop.dongsoop.notification.constant.NotificationType> type = createEnum("type", com.dongsoop.dongsoop.notification.constant.NotificationType.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final StringPath value = createString("value");

    public QNotificationDetails(String variable) {
        super(NotificationDetails.class, forVariable(variable));
    }

    public QNotificationDetails(Path<? extends NotificationDetails> path) {
        super(path.getType(), path.getMetadata());
    }

    public QNotificationDetails(PathMetadata metadata) {
        super(NotificationDetails.class, metadata);
    }

}

