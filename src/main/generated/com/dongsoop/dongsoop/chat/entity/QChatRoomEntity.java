package com.dongsoop.dongsoop.chat.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QChatRoomEntity is a Querydsl query type for ChatRoomEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QChatRoomEntity extends EntityPathBase<ChatRoomEntity> {

    private static final long serialVersionUID = 1795153784L;

    public static final QChatRoomEntity chatRoomEntity = new QChatRoomEntity("chatRoomEntity");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final BooleanPath isGroupChat = createBoolean("isGroupChat");

    public final DateTimePath<java.time.LocalDateTime> lastActivityAt = createDateTime("lastActivityAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> managerId = createNumber("managerId", Long.class);

    public final SetPath<Long, NumberPath<Long>> participants = this.<Long, NumberPath<Long>>createSet("participants", Long.class, NumberPath.class, PathInits.DIRECT2);

    public final StringPath roomId = createString("roomId");

    public final StringPath title = createString("title");

    public QChatRoomEntity(String variable) {
        super(ChatRoomEntity.class, forVariable(variable));
    }

    public QChatRoomEntity(Path<? extends ChatRoomEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QChatRoomEntity(PathMetadata metadata) {
        super(ChatRoomEntity.class, metadata);
    }

}

