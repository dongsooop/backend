package com.dongsoop.dongsoop.notification.setting.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QNotificationSettingId is a Querydsl query type for NotificationSettingId
 */
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QNotificationSettingId extends BeanPath<NotificationSettingId> {

    private static final long serialVersionUID = -166656919L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QNotificationSettingId notificationSettingId = new QNotificationSettingId("notificationSettingId");

    public final com.dongsoop.dongsoop.memberdevice.entity.QMemberDevice device;

    public final EnumPath<com.dongsoop.dongsoop.notification.constant.NotificationType> notificationType = createEnum("notificationType", com.dongsoop.dongsoop.notification.constant.NotificationType.class);

    public QNotificationSettingId(String variable) {
        this(NotificationSettingId.class, forVariable(variable), INITS);
    }

    public QNotificationSettingId(Path<? extends NotificationSettingId> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QNotificationSettingId(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QNotificationSettingId(PathMetadata metadata, PathInits inits) {
        this(NotificationSettingId.class, metadata, inits);
    }

    public QNotificationSettingId(Class<? extends NotificationSettingId> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.device = inits.isInitialized("device") ? new com.dongsoop.dongsoop.memberdevice.entity.QMemberDevice(forProperty("device"), inits.get("device")) : null;
    }

}

