package com.dongsoop.dongsoop.chat.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QChatMessageEntity is a Querydsl query type for ChatMessageEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QChatMessageEntity extends EntityPathBase<ChatMessageEntity> {

    private static final long serialVersionUID = -954930768L;

    public static final QChatMessageEntity chatMessageEntity = new QChatMessageEntity("chatMessageEntity");

    public final StringPath content = createString("content");

    public final StringPath messageId = createString("messageId");

    public final StringPath roomId = createString("roomId");

    public final NumberPath<Long> senderId = createNumber("senderId", Long.class);

    public final DateTimePath<java.time.LocalDateTime> timestamp = createDateTime("timestamp", java.time.LocalDateTime.class);

    public final EnumPath<MessageType> type = createEnum("type", MessageType.class);

    public QChatMessageEntity(String variable) {
        super(ChatMessageEntity.class, forVariable(variable));
    }

    public QChatMessageEntity(Path<? extends ChatMessageEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QChatMessageEntity(PathMetadata metadata) {
        super(ChatMessageEntity.class, metadata);
    }

}

